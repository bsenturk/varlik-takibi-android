package com.xptlabs.varliktakibi.widget

import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.data.local.entity.totalCost
import com.xptlabs.varliktakibi.data.local.entity.totalValue
import com.xptlabs.varliktakibi.data.remote.AssetPrice
import com.xptlabs.varliktakibi.data.repo.ProLock
import com.xptlabs.varliktakibi.market.MarketDataStore
import kotlin.math.abs

/** Widget'ın çizeceği durum. iOS `PortfolioEntry.State` karşılığı. */
sealed interface WidgetState {
    /**
     * Pro değil. Kart varsa kullanıcının *kendi* portföyü tutarları maskeli
     * çizilir — neyi kaçırdığını görmek jenerik bir kilitten güçlü. null ise
     * gösterilecek bir şey yok (boş portföyde maske yanıltıcı olurdu).
     */
    data class Locked(val card: WidgetCard?) : WidgetState
    /** Henüz hiç portföy yok (widget uygulama açılmadan eklendi). */
    data object NeedsApp : WidgetState
    data class Ready(val card: WidgetCard) : WidgetState
}

data class WidgetCard(
    val name: String,
    val colorHex: String,
    val total: String,
    val profitLoss: String,
    val percent: Double,
    val isPositive: Boolean,
    val hasProfitLoss: Boolean,
    val rows: List<Row>,
    /** Fiyatlar çekilemedi; son bilinen fiyat kullanıldı. */
    val stale: Boolean,
    /** Pro değil: aynı düzen, tutarlar maskeli, kâr/zarar yerine "Pro ile aç". */
    val isLocked: Boolean,
    val masked: Boolean
) {
    data class Row(val name: String, val value: String, val percent: Double, val tintHex: String)
}

/**
 * Seçili portföyün widget kartı. Saf fonksiyon: veriyi çağıran topluyor.
 *
 * @param prices widget'ın kendi çektiği güncel fiyat satırları; null ise çekim
 *   başarısız — varlıkların son bilinen fiyatına düşülür ve kart "bayat" işaretlenir.
 */
fun buildWidgetState(
    portfolios: List<PortfolioEntity>,
    assets: List<AssetEntity>,
    selectedPortfolioId: String?,
    maskedPortfolioIds: Set<String>,
    currency: Currency,
    isPro: Boolean,
    prices: List<AssetPrice>?
): WidgetState {
    if (portfolios.isEmpty()) return if (isPro) WidgetState.NeedsApp else WidgetState.Locked(null)

    // Abonelik bitince kilitlenen portföy widget'a sızmamalı — uygulamadaki
    // kilit ana ekranda da geçerli.
    val lockedIds = ProLock.lockedPortfolioIds(portfolios, isPro)
    val portfolio = portfolios.firstOrNull { it.id == selectedPortfolioId && it.id !in lockedIds }
        ?: portfolios.firstOrNull { it.isGeneral }
        ?: portfolios.first()
    val scoped = if (portfolio.isGeneral) assets.filter { it.portfolioId !in lockedIds }
    else assets.filter { it.portfolioId == portfolio.id }
    val holdings = scoped.filter { !ProLock.isLocked(it, lockedIds, isPro) }

    if (!isPro && holdings.isEmpty()) return WidgetState.Locked(null)

    // Kilitliyken fiyat zaten çekilmiyor: tutarlar maskeli.
    val priced = holdings.map { asset ->
        val live = if (asset.assetType.isManual || prices == null) null
        else MarketDataStore.tryPriceIn(prices, asset.symbol)
        asset.copy(currentPrice = live ?: asset.currentPrice)
    }
    val metrics = PortfolioMetrics.compute(priced)

    // Kur yoksa TL'de göster: yanlış kurla çevirmek sessizce yanlış tutar olurdu.
    val rate = if (currency == Currency.TRY) 1.0
    else prices?.let { MarketDataStore.tryPriceIn(it, currency.code) }
    val shownCurrency = if (rate != null && rate > 0) currency else Currency.TRY
    val divisor = if (shownCurrency == Currency.TRY) 1.0 else rate!!

    val locked = !isPro
    val masked = locked || portfolio.id in maskedPortfolioIds
    fun money(tryValue: Double) =
        if (masked) TrFormat.MASK else TrFormat.money(tryValue / divisor, shownCurrency)

    val rows = priced
        .sortedByDescending { it.totalValue ?: 0.0 }
        .take(3)
        .map { asset ->
            val value = asset.totalValue ?: 0.0
            val cost = asset.totalCost
            WidgetCard.Row(
                // Aynı enstrüman farklı yerlerde tutulabiliyor; iki özdeş satır olmasın.
                name = if (asset.location.isEmpty()) asset.name else "${asset.name} · ${asset.location}",
                value = money(value),
                percent = if (cost > 0) (value - cost) / cost * 100.0 else 0.0,
                tintHex = asset.assetType.tintHex
            )
        }

    val card = WidgetCard(
        name = portfolio.name,
        colorHex = portfolio.colorHex,
        total = money(metrics.totalValue),
        profitLoss = (if (metrics.profitLoss < 0) "-" else "+") + money(abs(metrics.profitLoss)),
        percent = metrics.profitLossPercent,
        isPositive = metrics.isPositive,
        hasProfitLoss = metrics.hasProfitLoss,
        rows = rows,
        stale = !locked && prices == null && holdings.isNotEmpty(),
        isLocked = locked,
        masked = masked
    )
    return if (locked) WidgetState.Locked(card) else WidgetState.Ready(card)
}
