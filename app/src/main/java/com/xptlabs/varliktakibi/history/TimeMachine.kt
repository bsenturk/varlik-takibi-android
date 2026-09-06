package com.xptlabs.varliktakibi.history

import android.util.Log
import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.ext.parseTimestampOrNull
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.dao.SnapshotDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity
import com.xptlabs.varliktakibi.data.remote.MarketDataService
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Eksik günlük [PortfolioSnapshotEntity] kayıtlarını sessizce yeniden kurar,
 * böylece geçmiş grafiği kronolojik boşluk olmadan çizilir.
 *
 * Algoritma (portföy başına):
 *  1. Boşluk tespiti  — son anlık görüntüden bugüne eksik günler.
 *  2. İşlem tekrarı   — o gün elde tutulan miktar, işlem geçmişinden.
 *  3. Fiyat çekme     — o günler ve semboller için geçmiş kapanışlar.
 *  4. Değerleme       — miktar × fiyat, gün bazında toplanır.
 *  5. Kaydetme        — her yeniden kurulan gün için bir anlık görüntü.
 *
 * iOS `PortfolioCalculatorService.swift` portu.
 */
@Singleton
class TimeMachine @Inject constructor(
    private val repository: PortfolioRepository,
    private val snapshotDao: SnapshotDao,
    private val historyDao: HistoryDao,
    private val marketData: MarketDataService
) {

    /** Tüm gerçek portföyler için en iyi çaba; biri patlarsa diğerleri devam eder. */
    suspend fun reconstructAll() {
        val portfolios = repository.portfolios().filter { !it.isGeneral }
        for (portfolio in portfolios) {
            runCatching { reconstruct(portfolio) }
                .onFailure { Log.w(TAG, "Skipped ${portfolio.name}: ${it.message}") }
        }
    }

    suspend fun reconstruct(portfolio: PortfolioEntity) {
        val assets = repository.assetsIn(portfolio.id)
        if (assets.isEmpty()) return

        // ── 1. Boşluk tespiti ────────────────────────────────────────────────
        val today = Days.today()
        val anchor = snapshotDao.latestDay(portfolio.id)
            ?: Days.plus(assets.minOf { Days.startOf(it.dateAdded) }, -1)
        val missing = Days.range(Days.plus(anchor, 1), today)
        if (missing.isEmpty()) return

        // ── 3. Geçmiş fiyatları tek seferde çek ──────────────────────────────
        val symbols = assets.map { it.symbol }.distinct()
        val series = priceSeries(
            symbols = symbols,
            from = Instant.ofEpochMilli(missing.first()),
            to = Instant.ofEpochMilli(missing.last())
        )

        // ── 2 + 4 + 5: her eksik günü oynat, değerle ve kaydet ───────────────
        val amountsBySymbol = assets.associate { it.symbol to historicalAmounts(it) }
        val snapshots = mutableListOf<PortfolioSnapshotEntity>()

        for (day in missing) {
            var total = 0.0
            for (asset in assets) {
                val amount = amountOn(day, asset, amountsBySymbol[asset.symbol].orEmpty())
                if (amount <= 0) continue
                val price = priceOn(asset.symbol, day, series) ?: continue
                total += amount * price
            }
            // Değerlenemeyen günü kaydetmiyoruz: 0 yazmak grafiği tabana çakar ve
            // aralık yüzdesini bozar. Boşluk zararsız — fiyat sonradan gelirse
            // bir sonraki açılışta tekrar denenir.
            if (total > 0) {
                snapshots += PortfolioSnapshotEntity(
                    portfolioId = portfolio.id,
                    day = day,
                    totalValue = total
                )
            }
        }

        if (snapshots.isNotEmpty()) snapshotDao.upsertAll(snapshots)
        Log.d(TAG, "Reconstructed ${snapshots.size}/${missing.size} day(s) for ${portfolio.name}")
    }

    // ── Adım 2: işlem tekrarı ────────────────────────────────────────────────

    /** (gün, o günün sonundaki toplam miktar) çiftleri, artan sırada. */
    private suspend fun historicalAmounts(asset: AssetEntity): List<Pair<Long, Double>> =
        historyDao.transactions(asset.symbol)
            .map { Days.startOf(it.date) to it.totalAmount }
            .sortedBy { it.first }

    /**
     * Varlığın [day] sonunda elde tutulan miktarı: o güne kadarki son işlemin
     * çalışan toplamı. İşlem geçmişi yoksa varlık eklendiği günden beri aynı
     * miktarın tutulduğu varsayılır.
     */
    private fun amountOn(
        day: Long,
        asset: AssetEntity,
        transactions: List<Pair<Long, Double>>
    ): Double {
        val last = transactions.lastOrNull { it.first <= day }
        if (last != null) return maxOf(0.0, last.second)
        return if (Days.startOf(asset.dateAdded) <= day) asset.amount else 0.0
    }

    // ── Adım 3/4: fiyat arama ────────────────────────────────────────────────

    /** sembol -> [(gün, fiyat)] artan sırada; ileri taşıma araması için. */
    private suspend fun priceSeries(
        symbols: List<String>,
        from: Instant,
        to: Instant
    ): Map<String, List<Pair<Long, Double>>> {
        val remote = runCatching { marketData.fetchHistoricalPrices(symbols, from, to) }
            .onFailure { Log.w(TAG, "Historical price fetch failed: ${it.message}") }
            .getOrDefault(emptyList())

        val series = mutableMapOf<String, MutableList<Pair<Long, Double>>>()

        // Yerel geçmiş her zaman var; uzak veri onu zenginleştiriyor. Backend
        // assets_prices'ı (symbol, currency) üzerinden upsert ettiği için orada
        // genelde tek bir güncel satır bulunur — asıl kaynak yerel anlık görüntüler.
        for (symbol in symbols) {
            val local = historyDao.priceHistory(symbol).map { it.day to it.price }
            if (local.isNotEmpty()) series.getOrPut(symbol) { mutableListOf() }.addAll(local)
        }
        for (row in remote) {
            val day = row.updatedAt.parseTimestampOrNull()?.toEpochMilli()?.let(Days::startOf)
                ?: continue
            series.getOrPut(row.symbol) { mutableListOf() }.add(day to row.price)
        }

        return series.mapValues { (_, points) ->
            points.sortedBy { it.first }.distinctBy { it.first }
        }
    }

    /**
     * [day] için kapanış fiyatı. Hafta sonu/tatilde piyasa kapalı olduğundan
     * o güne kadarki en son fiyat ileri taşınır. Gün ilk bilinen fiyattan
     * önceyse en eski fiyat kullanılır.
     */
    private fun priceOn(
        symbol: String,
        day: Long,
        series: Map<String, List<Pair<Long, Double>>>
    ): Double? {
        val points = series[symbol]?.takeIf { it.isNotEmpty() } ?: return null
        return points.lastOrNull { it.first <= day }?.second ?: points.first().second
    }

    private companion object {
        const val TAG = "TimeMachine"
    }
}
