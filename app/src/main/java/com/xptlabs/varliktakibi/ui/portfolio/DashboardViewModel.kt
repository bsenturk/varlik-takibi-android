package com.xptlabs.varliktakibi.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.data.local.entity.category
import com.xptlabs.varliktakibi.data.local.entity.totalCost
import com.xptlabs.varliktakibi.data.local.entity.totalValue
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.history.HistoryRecorder
import com.xptlabs.varliktakibi.market.MarketDataStore
import com.xptlabs.varliktakibi.ui.common.AssetRowItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val portfolios: List<PortfolioEntity> = emptyList(),
    val selectedPortfolio: PortfolioEntity? = null,
    val rows: List<AssetRowItem> = emptyList(),
    val metrics: PortfolioMetrics = PortfolioMetrics.ZERO,
    val currency: Currency = Currency.TRY,
    val valuesMasked: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isPro: Boolean = false
) {
    val isGeneralSelected: Boolean get() = selectedPortfolio?.isGeneral == true
    val isEmpty: Boolean get() = rows.isEmpty()

    val countLabel: String
        get() = if (isGeneralSelected) "${rows.size} kategori" else "${rows.size} varlık"
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val assetDao: AssetDao,
    private val historyDao: HistoryDao,
    private val history: HistoryRecorder,
    private val prefs: AppPreferences,
    private val market: MarketDataStore,
    private val purchaseManager: PurchaseManager,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    /** Sembol → son 30 günün fiyat serisi; sparkline için, varlıklar değişince yenilenir. */
    private val sparklines = MutableStateFlow<Map<String, List<Double>>>(emptyMap())

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observePortfolios(),
        repository.observeAssets(),
        market.prices,
        combine(
            prefs.selectedPortfolioId,
            prefs.selectedCurrency,
            prefs.maskedPortfolioIds,
            purchaseManager.isPro
        ) { id, currency, masked, isPro -> Prefs(id, currency, masked, isPro) },
        combine(market.isRefreshing, market.error, sparklines) { refreshing, error, sparks ->
            Triple(refreshing, error, sparks)
        }
    ) { portfolios, assets, _, prefsSnapshot, (refreshing, error, sparks) ->
        buildState(portfolios, assets, prefsSnapshot, refreshing, error, sparks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private data class Prefs(
        val selectedId: String?,
        val currency: Currency,
        val maskedIds: Set<String>,
        val isPro: Boolean
    )

    init {
        // Canlı fiyat geldikçe varlık satırlarına yaz — iOS'un updateAssetPrices'ı.
        viewModelScope.launch {
            market.prices.collect { persistLivePrices() }
        }
        viewModelScope.launch {
            repository.observeAssets().collect { loadSparklines(it) }
        }
    }

    private suspend fun buildState(
        portfolios: List<PortfolioEntity>,
        assets: List<AssetEntity>,
        prefsSnapshot: Prefs,
        refreshing: Boolean,
        error: String?,
        sparks: Map<String, List<Double>>
    ): DashboardUiState {
        val selected = portfolios.firstOrNull { it.id == prefsSnapshot.selectedId }
            ?: portfolios.firstOrNull { it.isGeneral }
            ?: portfolios.firstOrNull()

        val scoped = when {
            selected == null -> emptyList()
            selected.isGeneral -> assets
            else -> assets.filter { it.portfolioId == selected.id }
        }

        return DashboardUiState(
            portfolios = portfolios,
            selectedPortfolio = selected,
            rows = if (selected?.isGeneral == true) categoryRows(scoped, sparks)
            else assetRows(scoped, sparks),
            metrics = PortfolioMetrics.compute(scoped),
            currency = prefsSnapshot.currency,
            valuesMasked = selected != null && selected.id in prefsSnapshot.maskedIds,
            isRefreshing = refreshing,
            errorMessage = error,
            isPro = prefsSnapshot.isPro
        )
    }

    // ── Satır üretimi ────────────────────────────────────────────────────────

    private fun assetRows(assets: List<AssetEntity>, sparks: Map<String, List<Double>>) =
        assets
            .sortedByDescending { it.totalValue ?: 0.0 }
            .map { asset ->
                val type = asset.assetType
                AssetRowItem(
                    id = asset.id,
                    title = asset.name,
                    subtitle = "${com.xptlabs.varliktakibi.core.format.TrFormat.amount(asset.amount)} ${asset.unit}",
                    value = asset.totalValue,
                    changePercent = profitLossPercent(asset),
                    sparkline = sparks[asset.symbol].orEmpty(),
                    icon = type.icon,
                    tintHex = type.tintHex,
                    flag = type.flag,
                    assetId = asset.id
                )
            }

    /** "Genel" portföyde satırlar varlık değil kategori. */
    private fun categoryRows(assets: List<AssetEntity>, sparks: Map<String, List<Double>>) =
        assets.groupBy { it.category }
            .mapNotNull { (category, items) ->
                val value = items.sumOf { it.totalValue ?: 0.0 }
                if (value <= 0) return@mapNotNull null

                val cost = items.sumOf { it.totalCost }
                val percent = if (cost > 0) (value - cost) / cost * 100.0 else 0.0

                AssetRowItem(
                    id = "cat-${category.name}",
                    title = category.displayName,
                    subtitle = "${items.size} varlık",
                    value = value,
                    changePercent = percent,
                    sparkline = aggregateSparkline(items, sparks),
                    icon = category.icon,
                    tintHex = category.tintHex,
                    assetId = null
                )
            }
            .sortedByDescending { it.value ?: 0.0 }

    /** Kategori satırında tüm varlıkların gün bazında toplanmış değer serisi. */
    private fun aggregateSparkline(
        items: List<AssetEntity>,
        sparks: Map<String, List<Double>>
    ): List<Double> {
        val series = items.mapNotNull { asset ->
            sparks[asset.symbol]?.map { it * asset.amount }
        }
        if (series.isEmpty()) return emptyList()

        // Serilerin uzunluğu farklı olabilir (varlıklar farklı günlerde eklendi);
        // en kısa olana hizalayıp son N günü topluyoruz.
        val length = series.minOf { it.size }
        if (length == 0) return emptyList()
        return (0 until length).map { i ->
            series.sumOf { it[it.size - length + i] }
        }
    }

    private fun profitLossPercent(asset: AssetEntity): Double {
        val price = asset.currentPrice ?: return 0.0
        if (asset.costBasis <= 0) return 0.0
        return (price - asset.costBasis) / asset.costBasis * 100.0
    }

    // ── Yan etkiler ──────────────────────────────────────────────────────────

    /**
     * Canlı fiyatları varlık satırlarına yazar. Karşılaştırma oransal: sabit bir
     * 0,01 TL eşiği, birim fiyatı kuruşlarla ölçülen fonların (0,35 TL) tüm
     * günlük hareketini yutup kâr/zararı sıfırda dondururdu.
     */
    private suspend fun persistLivePrices() {
        val assets = assetDao.getAll()
        if (assets.isEmpty()) return

        val updated = assets.mapNotNull { asset ->
            val price = market.tryPrice(asset.symbol) ?: return@mapNotNull null
            val current = asset.currentPrice
            val threshold = maxOf(current ?: 0.0, 1.0) * 1e-6
            if (current != null && kotlin.math.abs(price - current) <= threshold) return@mapNotNull null
            asset.copy(currentPrice = price, lastUpdated = System.currentTimeMillis())
        }
        if (updated.isNotEmpty()) assetDao.updateAll(updated)

        recordTodaySnapshotOnce(assets)
    }

    /**
     * Günlük fiyat anlık görüntüsünü uygulama açıkken de yazar. SnapshotWorker
     * zaten günde bir çalışıyor ama uygulamayı sık açan kullanıcının grafiği
     * işin tetiklenmesini beklemesin. Gün başına bir kez — her fiyat
     * yenilemesinde DB'ye yazmanın anlamı yok.
     */
    private suspend fun recordTodaySnapshotOnce(assets: List<AssetEntity>) {
        val today = Days.today()
        if (prefs.lastSnapshotDay.first() == today) return

        var recorded = false
        assets.forEach { asset ->
            val price = market.tryPrice(asset.symbol) ?: return@forEach
            history.recordPrice(asset.symbol, today, price, asset.amount)
            recorded = true
        }
        if (recorded) prefs.setLastSnapshotDay(today)
    }

    private suspend fun loadSparklines(assets: List<AssetEntity>) {
        val symbols = assets.map { it.symbol }.distinct()
        sparklines.value = symbols.associateWith { symbol ->
            historyDao.priceHistory(symbol).takeLast(SPARKLINE_DAYS).map { it.price }
        }
    }

    // ── Kullanıcı eylemleri ──────────────────────────────────────────────────

    fun refresh() = viewModelScope.launch { market.refresh() }

    fun selectPortfolio(portfolio: PortfolioEntity) = viewModelScope.launch {
        prefs.setSelectedPortfolioId(portfolio.id)
    }

    fun setCurrency(currency: Currency) = viewModelScope.launch {
        prefs.setSelectedCurrency(currency)
        analytics.logCurrencyChanged(currency.code)
    }

    fun toggleMask() = viewModelScope.launch {
        uiState.value.selectedPortfolio?.let { prefs.togglePortfolioMask(it.id) }
    }

    fun createPortfolio(name: String, color: PortfolioColor) = viewModelScope.launch {
        val created = repository.createPortfolio(name, color)
        prefs.setSelectedPortfolioId(created.id)
    }

    fun updatePortfolio(portfolio: PortfolioEntity, name: String, color: PortfolioColor) =
        viewModelScope.launch { repository.updatePortfolio(portfolio, name, color) }

    fun deletePortfolio(portfolio: PortfolioEntity) = viewModelScope.launch {
        repository.deletePortfolio(portfolio)
        repository.portfolios().firstOrNull { it.isGeneral }
            ?.let { prefs.setSelectedPortfolioId(it.id) }
    }

    fun deleteAsset(assetId: String) = viewModelScope.launch {
        val asset = assetDao.getById(assetId) ?: return@launch
        repository.deleteAsset(asset)
        analytics.logAssetDeleted(asset.category.name, asset.symbol)
    }

    /** Yeni portföy açılabilir mi — ücretsiz kullanıcıda limit var. */
    fun canCreatePortfolio(): Boolean {
        val state = uiState.value
        if (state.isPro) return true
        return state.portfolios.count { !it.isGeneral } < PortfolioRepository.FREE_PORTFOLIO_LIMIT
    }

    /** TL tutarını seçili para birimine çevirir; kur yoksa TL'de bırakır. */
    fun convert(amountTry: Double, currency: Currency): Double {
        if (currency == Currency.TRY) return amountTry
        val rate = market.tryPrice(currency.code) ?: return amountTry
        return if (rate > 0) amountTry / rate else amountTry
    }

    fun clearError() = market.clearError()

    private companion object {
        const val SPARKLINE_DAYS = 30
    }
}
