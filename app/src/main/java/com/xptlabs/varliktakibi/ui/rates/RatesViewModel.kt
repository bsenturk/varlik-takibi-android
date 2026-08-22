package com.xptlabs.varliktakibi.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.market.Instrument
import com.xptlabs.varliktakibi.market.MarketDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class RatesTab(val label: String, val category: AssetCategory) {
    GOLD("Altın", AssetCategory.GOLD),
    CURRENCY("Döviz", AssetCategory.CURRENCY)
}

data class RatesUiState(
    val tab: RatesTab = RatesTab.GOLD,
    val rates: List<Instrument> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdateLabel: String = "—"
)

@HiltViewModel
class RatesViewModel @Inject constructor(
    private val market: MarketDataStore
) : ViewModel() {

    private val tab = MutableStateFlow(RatesTab.GOLD)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<RatesUiState> = combine(
        market.prices,
        market.isRefreshing,
        market.error,
        market.lastUpdate,
        combine(tab, query) { t, q -> t to q }
    ) { _, refreshing, error, lastUpdate, (selectedTab, searchQuery) ->
        RatesUiState(
            tab = selectedTab,
            rates = rates(selectedTab, searchQuery),
            query = searchQuery,
            isRefreshing = refreshing,
            errorMessage = error,
            lastUpdateLabel = lastUpdateLabel(lastUpdate)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatesUiState())

    private fun rates(tab: RatesTab, query: String): List<Instrument> {
        val base = when (tab) {
            // Gümüş kendi kategorisinde ama kullanıcı için "değerli maden" —
            // iOS'ta da Altın sekmesinde listeleniyor.
            RatesTab.GOLD -> market.instruments(AssetCategory.GOLD) +
                market.instruments(AssetCategory.SILVER)

            // 1 TRY = 1 TRY satırının kur listesinde işi yok.
            RatesTab.CURRENCY -> market.instruments(AssetCategory.CURRENCY)
                .filterNot { it.symbol == "TRY" }
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) return base
        return base.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.symbol.contains(trimmed, ignoreCase = true)
        }
    }

    private fun lastUpdateLabel(instant: Instant?): String {
        if (instant == null) return "—"
        val minutes = Duration.between(instant, Instant.now()).toMinutes()
        return when {
            minutes < 1 -> "Canlı"
            minutes < 60 -> "$minutes dk önce"
            else -> "${minutes / 60} sa önce"
        }
    }

    fun setTab(value: RatesTab) { tab.value = value }
    fun setQuery(value: String) { query.value = value }
    fun refresh() = viewModelScope.launch { market.refresh() }
}
