package com.xptlabs.varliktakibi.ui.addasset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.repo.AssetEditor
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.data.repo.ProLock
import com.xptlabs.varliktakibi.market.Instrument
import com.xptlabs.varliktakibi.market.MarketDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Akışın üç adımı: kategori → enstrüman → miktar. */
sealed interface AddAssetStep {
    data object Category : AddAssetStep
    data class InstrumentList(val category: AssetCategory) : AddAssetStep
    data class Amount(val instrument: Instrument) : AddAssetStep
}

data class AddAssetUiState(
    val step: AddAssetStep = AddAssetStep.Category,
    val portfolios: List<PortfolioEntity> = emptyList(),
    val selectedPortfolio: PortfolioEntity? = null,
    val instruments: List<Instrument> = emptyList(),
    val query: String = "",
    val isSearchingFunds: Boolean = false,
    val isPro: Boolean = false,
    val errorMessage: String? = null,
    /** Kaydetme bitti — ekran kapanmalı; birleştirme olduysa true. */
    val savedAsMerge: Boolean? = null
)

@HiltViewModel
class AddAssetViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val editor: AssetEditor,
    private val market: MarketDataStore,
    private val prefs: AppPreferences,
    private val purchaseManager: PurchaseManager,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAssetUiState())
    val uiState: StateFlow<AddAssetUiState> = _uiState.asStateFlow()

    private var fundSearchJob: Job? = null

    // ── Huni durumu ──────────────────────────────────────────────────────────
    // Akışın nerede koptuğunu yazabilmek için ulaşılan **en derin** adım ve o
    // adımdaki kategori tutuluyor; kullanıcı geri dönse de en derin nokta kalır.
    private var source = SOURCE_MANUAL
    private var deepestStep = STEP_CATEGORY
    private var deepestCategory: AssetCategory? = null

    init {
        // Liste akıştan izleniyor: ViewModel ekrandan uzun yaşıyor, tek seferlik
        // okumada aradan yeni açılan portföy hiç görünmüyordu.
        viewModelScope.launch {
            combine(repository.observePortfolios(), purchaseManager.isPro) { all, isPro ->
                // Kilitli portföyler listelenmiyor; aksi hâlde kullanıcı kilidin
                // arkasına yeni varlık yazabilirdi.
                val lockedIds = ProLock.lockedPortfolioIds(all, isPro)
                all.filter { !it.isGeneral && it.id !in lockedIds }
            }.collect { portfolios ->
                // Varlık, o an seçili portföye eklenir; "Genel" seçiliyse ilk
                // gerçek portföye — Genel bir toplayıcı, varlık tutmaz.
                val preferredId = prefs.selectedPortfolioId.first()
                _uiState.update { state ->
                    val stillThere = state.selectedPortfolio
                        ?.let { current -> portfolios.firstOrNull { it.id == current.id } }
                    state.copy(
                        portfolios = portfolios,
                        selectedPortfolio = stillThere
                            ?: portfolios.firstOrNull { it.id == preferredId }
                            ?: portfolios.firstOrNull()
                    )
                }
            }
        }
        viewModelScope.launch {
            purchaseManager.isPro.collect { pro -> _uiState.update { it.copy(isPro = pro) } }
        }
    }

    /** Ekran açıldığında bir kez; [source] "onboarding" ya da "manual". */
    fun onOpened(source: String) {
        this.source = source
        analytics.logAddAssetOpened(source)
    }

    // ── Gezinme ──────────────────────────────────────────────────────────────

    /** @return kategori premium ve kullanıcı Pro değilse false (paywall açılmalı). */
    fun openCategory(category: AssetCategory): Boolean {
        if (category.isPremium && !_uiState.value.isPro) {
            analytics.logPremiumCategoryLocked(category.name)
            return false
        }
        deepestStep = STEP_TYPE_LIST
        deepestCategory = category
        analytics.logAddAssetCategorySelected(category.name, source)
        _uiState.update {
            it.copy(
                step = AddAssetStep.InstrumentList(category),
                query = "",
                instruments = market.instruments(category)
            )
        }
        return true
    }

    fun openInstrument(instrument: Instrument) {
        deepestStep = STEP_AMOUNT
        analytics.logAddAssetInstrumentSelected(
            category = instrument.category.name,
            symbol = instrument.symbol,
            source = source
        )
        _uiState.update { it.copy(step = AddAssetStep.Amount(instrument)) }
    }

    /** @return false ise en baştayız, ekran kapanmalı. */
    fun goBack(): Boolean = when (val step = _uiState.value.step) {
        is AddAssetStep.Category -> false
        is AddAssetStep.InstrumentList -> {
            _uiState.update { it.copy(step = AddAssetStep.Category, query = "") }
            true
        }
        is AddAssetStep.Amount -> {
            _uiState.update { it.copy(step = AddAssetStep.InstrumentList(step.instrument.category)) }
            true
        }
    }

    // ── Arama ────────────────────────────────────────────────────────────────

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        val category = (_uiState.value.step as? AddAssetStep.InstrumentList)?.category ?: return

        // Fonlar dışındaki kategorilerde arama tamamen yerel — katalog zaten elde.
        if (category != AssetCategory.FUND) return

        fundSearchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(isSearchingFunds = false) }
            return
        }
        fundSearchJob = viewModelScope.launch {
            delay(FUND_SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchingFunds = true) }
            val results = market.searchFunds(query)
            _uiState.update { it.copy(instruments = results, isSearchingFunds = false) }
        }
    }

    /** Arama metnine göre süzülmüş enstrümanlar. */
    fun filteredInstruments(): List<Instrument> {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isEmpty()) return state.instruments
        return state.instruments.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
        }
    }

    fun selectPortfolio(portfolio: PortfolioEntity) {
        _uiState.update { it.copy(selectedPortfolio = portfolio) }
    }

    // ── Kaydetme ─────────────────────────────────────────────────────────────

    fun save(instrument: Instrument, amountText: String, purchasePriceText: String) {
        val amount = amountText.toDoubleOrNullTr()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Lütfen geçerli bir miktar girin.") }
            return
        }
        val portfolio = _uiState.value.selectedPortfolio
        if (portfolio == null) {
            _uiState.update { it.copy(errorMessage = "Lütfen bir portföy seçin.") }
            return
        }
        val cost = purchasePriceText.toDoubleOrNullTr()?.takeIf { it > 0 }

        viewModelScope.launch {
            runCatching {
                editor.addOrMerge(instrument, portfolio.id, amount, cost)
            }.onSuccess { merged ->
                analytics.logAssetAdded(
                    category = instrument.category.name,
                    symbol = instrument.symbol,
                    isMerge = merged,
                    hasPurchasePrice = cost != null,
                    source = source
                )
                _uiState.update { it.copy(savedAsMerge = merged) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Varlık eklenemedi.")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /**
     * Ekran kapanırken akışı başa alır. ViewModel Activity'ye bağlı olduğu için
     * sıfırlanmazsa bir sonraki açılışta `savedAsMerge` hâlâ doluydu ve ekran
     * açılır açılmaz kendini kapatıyordu.
     */
    fun resetFlow() {
        fundSearchJob?.cancel()
        // Kaydetmeden kapandıysa huninin nerede koptuğunu yaz.
        if (_uiState.value.savedAsMerge == null) {
            analytics.logAddAssetAbandoned(deepestStep, deepestCategory?.name, source)
        }
        deepestStep = STEP_CATEGORY
        deepestCategory = null
        viewModelScope.launch {
            val preferredId = prefs.selectedPortfolioId.first()
            _uiState.update { state ->
                state.copy(
                    step = AddAssetStep.Category,
                    instruments = emptyList(),
                    query = "",
                    isSearchingFunds = false,
                    errorMessage = null,
                    savedAsMerge = null,
                    selectedPortfolio = state.portfolios.firstOrNull { it.id == preferredId }
                        ?: state.portfolios.firstOrNull()
                )
            }
        }
    }

    /** Kullanıcı bir şey girmeden önce göstereceğimiz güncel piyasa fiyatı. */
    fun marketPrice(instrument: Instrument): Double =
        market.tryPrice(instrument.symbol) ?: instrument.priceTry

    companion object {
        const val SOURCE_ONBOARDING = "onboarding"
        const val SOURCE_MANUAL = "manual"

        private const val FUND_SEARCH_DEBOUNCE_MS = 350L
        private const val STEP_CATEGORY = "category"
        private const val STEP_TYPE_LIST = "type_list"
        private const val STEP_AMOUNT = "amount"
    }
}

/**
 * Türkçe biçimli sayıyı çözer. Virgül varsa nokta binlik ayracıdır ("1.234,56");
 * yoksa nokta ondalık ayracı sayılır ki elle "12.5" yazan kullanıcı 125 elde etmesin.
 */
internal fun String.toDoubleOrNullTr(): Double? {
    val text = trim()
    if (text.isEmpty()) return null
    return if (text.contains(',')) {
        text.replace(".", "").replace(',', '.').toDoubleOrNull()
    } else {
        text.toDoubleOrNull()
    }
}
