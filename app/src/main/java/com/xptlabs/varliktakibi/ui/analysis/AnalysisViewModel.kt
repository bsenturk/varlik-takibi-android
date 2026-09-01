package com.xptlabs.varliktakibi.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.dao.SnapshotDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.category
import com.xptlabs.varliktakibi.data.local.entity.totalValue
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.market.MarketDataStore
import com.xptlabs.varliktakibi.ui.common.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class TimeRange(val label: String, val days: Long) {
    WEEK("1H", 7),
    MONTH("1A", 30),
    QUARTER("3A", 90),
    YEAR("1Y", 365),
    ALL("Tümü", 3650)
}

data class DistributionSlice(
    val name: String,
    val value: Double,
    val percent: Double,
    val tintHex: String
)

data class MoverItem(
    val id: String,
    val name: String,
    val changePercent: Double,
    val tintHex: String
)

data class AnalysisUiState(
    val portfolios: List<PortfolioEntity> = emptyList(),
    val selectedPortfolio: PortfolioEntity? = null,
    val metrics: PortfolioMetrics = PortfolioMetrics.ZERO,
    val currency: Currency = Currency.TRY,
    val valuesMasked: Boolean = false,
    val range: TimeRange = TimeRange.MONTH,
    val chart: List<ChartPoint> = emptyList(),
    val distribution: List<DistributionSlice> = emptyList(),
    val gainers: List<MoverItem> = emptyList(),
    val losers: List<MoverItem> = emptyList(),
    val isEmpty: Boolean = true
) {
    /** Seçili aralıktaki toplam değişim yüzdesi — grafiğin ilk ve son noktası. */
    val rangeChangePercent: Double
        get() {
            val first = chart.firstOrNull()?.value ?: return 0.0
            val last = chart.lastOrNull()?.value ?: return 0.0
            return if (first > 0) (last - first) / first * 100.0 else 0.0
        }
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analytics: FirebaseAnalyticsManager,
    private val repository: PortfolioRepository,
    private val snapshotDao: SnapshotDao,
    private val historyDao: HistoryDao,
    private val prefs: AppPreferences,
    private val market: MarketDataStore
) : ViewModel() {

    private val range = MutableStateFlow(TimeRange.MONTH)

    /** Sembol → bugünden önceki son kapanış; "günlük değişim" hesabı için. */
    private val previousCloses = MutableStateFlow<Map<String, Double>>(emptyMap())

    val uiState: StateFlow<AnalysisUiState> = combine(
        repository.observePortfolios(),
        repository.observeAssets(),
        snapshotDao.observeAll(),
        combine(prefs.selectedPortfolioId, prefs.selectedCurrency, prefs.maskedPortfolioIds) {
            id, currency, masked -> Triple(id, currency, masked)
        },
        combine(range, previousCloses) { r, closes -> r to closes }
    ) { portfolios, assets, snapshots, (selectedId, currency, masked), (selectedRange, closes) ->
        val selected = portfolios.firstOrNull { it.id == selectedId }
            ?: portfolios.firstOrNull { it.isGeneral }
            ?: portfolios.firstOrNull()

        val scoped = when {
            selected == null -> emptyList()
            selected.isGeneral -> assets
            else -> assets.filter { it.portfolioId == selected.id }
        }

        val relevantPortfolioIds = when {
            selected == null -> emptySet()
            selected.isGeneral -> portfolios.filter { !it.isGeneral }.map { it.id }.toSet()
            else -> setOf(selected.id)
        }

        val metrics = PortfolioMetrics.compute(scoped)
        val movers = movers(scoped, closes)

        AnalysisUiState(
            portfolios = portfolios,
            selectedPortfolio = selected,
            metrics = metrics,
            currency = currency,
            valuesMasked = selected != null && selected.id in masked,
            range = selectedRange,
            chart = buildChart(snapshots, relevantPortfolioIds, scoped, metrics, selectedRange),
            distribution = distribution(scoped),
            gainers = movers.first,
            losers = movers.second,
            isEmpty = scoped.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisUiState())

    init {
        viewModelScope.launch {
            repository.observeAssets().collect { assets ->
                previousCloses.value = assets.map { it.symbol }.distinct().associateWith { symbol ->
                    historyDao.priceHistory(symbol).lastOrNull { it.day < Days.today() }?.price
                        ?: 0.0
                }
            }
        }
    }

    fun setRange(value: TimeRange) {
        if (range.value == value) return
        range.value = value
        analytics.logAnalysisRangeSelected(value.name)
    }

    fun selectPortfolio(portfolio: PortfolioEntity) = viewModelScope.launch {
        prefs.setSelectedPortfolioId(portfolio.id)
    }

    fun convert(amountTry: Double, currency: Currency): Double {
        if (currency == Currency.TRY) return amountTry
        val rate = market.tryPrice(currency.code) ?: return amountTry
        return if (rate > 0) amountTry / rate else amountTry
    }

    // ── Grafik ───────────────────────────────────────────────────────────────

    /**
     * Günlük portföy değeri serisi. Kaynak [com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity] —
     * geçmiş günler o günkü miktarlarla değerlendiği için bugünkü miktarlarla
     * geriye dönük hesaplamaktan doğru. "Genel"de tüm gerçek portföyler günlük
     * toplanır; son nokta canlı toplama sabitlenir.
     */
    private fun buildChart(
        snapshots: List<com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity>,
        portfolioIds: Set<String>,
        assets: List<AssetEntity>,
        metrics: PortfolioMetrics,
        range: TimeRange
    ): List<ChartPoint> {
        if (assets.isEmpty()) return emptyList()

        val today = Days.today()
        val rangeStart = Days.plus(today, -range.days)

        // Kullanıcının şu anki varlıklarını edinmeden önceki günleri çizme:
        // eski bir varlık kompozisyonundan kalan anlık görüntüler aralık
        // yüzdesini anlamsız hale getiriyor (iOS'ta −%94 gibi).
        val earliestHolding = assets.minOf { Days.startOf(it.dateAdded) }
        val start = maxOf(rangeStart, earliestHolding)

        val byDay = snapshots
            .filter { it.portfolioId in portfolioIds && it.day in start..today }
            .groupBy { it.day }
            .mapValues { (_, rows) -> rows.sumOf { it.totalValue } }
            .toMutableMap()

        // Anlık görüntü günde bir yazılıyor; son noktayı canlı değere sabitle.
        if (metrics.totalValue > 0) byDay[today] = metrics.totalValue

        // Değerlenemeyen gün (0) grafiği tabana çakar ve aralık yüzdesini bozar.
        val series = byDay.entries
            .filter { it.value > 0 }
            .sortedBy { it.key }
            .map { ChartPoint(it.key, it.value) }

        return downsample(series, range)
    }

    /** Uzun aralıklar okunabilir kalsın: 3A haftalık, 1Y/Tümü aylık kovalar. */
    private fun downsample(series: List<ChartPoint>, range: TimeRange): List<ChartPoint> {
        val unit = when (range) {
            TimeRange.WEEK, TimeRange.MONTH -> return series
            TimeRange.QUARTER -> ChronoUnit.WEEKS
            TimeRange.YEAR, TimeRange.ALL -> ChronoUnit.MONTHS
        }
        if (series.size <= 2) return series

        return series
            .groupBy { point ->
                val date = Days.toLocalDate(point.day)
                if (unit == ChronoUnit.WEEKS) date.minusDays((date.dayOfWeek.value - 1).toLong())
                else date.withDayOfMonth(1)
            }
            // Kovadaki en güncel nokta temsil eder.
            .map { (_, points) -> points.maxBy { it.day } }
            .sortedBy { it.day }
    }

    // ── Dağılım ──────────────────────────────────────────────────────────────

    /** Varlık sınıfına göre dağılım: aynı sınıftaki tüm enstrümanlar tek dilim. */
    private fun distribution(assets: List<AssetEntity>): List<DistributionSlice> {
        val total = assets.sumOf { it.totalValue ?: 0.0 }
        if (total <= 0) return emptyList()

        return assets.groupBy { it.category }
            .mapNotNull { (category, items) ->
                val value = items.sumOf { it.totalValue ?: 0.0 }
                if (value <= 0) return@mapNotNull null
                DistributionSlice(
                    name = category.displayName,
                    value = value,
                    percent = value / total * 100.0,
                    tintHex = category.tintHex
                )
            }
            .sortedByDescending { it.value }
    }

    // ── Yükselen / düşen ─────────────────────────────────────────────────────

    /**
     * Günlük değişim varlık başına **bir kez** hesaplanır; doğrudan sıralama
     * karşılaştırıcısında hesaplamak n log n sorgu açardı.
     */
    private fun movers(
        assets: List<AssetEntity>,
        previousCloses: Map<String, Double>
    ): Pair<List<MoverItem>, List<MoverItem>> {
        val changes = assets.mapNotNull { asset ->
            val price = asset.currentPrice ?: return@mapNotNull null
            val previous = previousCloses[asset.symbol]?.takeIf { it > 0 } ?: return@mapNotNull null
            MoverItem(
                id = asset.id,
                name = asset.name,
                changePercent = (price - previous) / previous * 100.0,
                tintHex = asset.category.tintHex
            )
        }

        return changes.filter { it.changePercent > 0.001 }
            .sortedByDescending { it.changePercent }
            .take(3) to
            changes.filter { it.changePercent < -0.001 }
                .sortedBy { it.changePercent }
                .take(3)
    }
}
