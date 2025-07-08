package com.xptlabs.varliktakibi.presentation.rates

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.domain.models.RateDisplayModel
import com.xptlabs.varliktakibi.managers.MarketDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RatesUiState(
    val currencyRates: List<RateDisplayModel> = emptyList(),
    val goldRates: List<RateDisplayModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RatesViewModel @Inject constructor(
    private val marketDataManager: MarketDataManager,
    val analyticsManager: FirebaseAnalyticsManager
) : ViewModel() {

    companion object {
        private const val TAG = "RatesViewModel"
    }

    private val _uiState = MutableStateFlow(RatesUiState())
    val uiState: StateFlow<RatesUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "RatesViewModel initialized")
        setupBindings()
        loadInitialData()
    }

    private fun setupBindings() {
        viewModelScope.launch {
            marketDataManager.currencyRates.collect { rates ->
                Log.d(TAG, "Currency rates updated: ${rates.size}")
                val displayModels = updateCurrencyRates(rates)
                _uiState.value = _uiState.value.copy(currencyRates = displayModels)
            }
        }

        viewModelScope.launch {
            marketDataManager.goldRates.collect { rates ->
                Log.d(TAG, "Gold rates updated: ${rates.size}")
                val displayModels = updateGoldRates(rates)
                _uiState.value = _uiState.value.copy(goldRates = displayModels)
            }
        }

        viewModelScope.launch {
            marketDataManager.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isRefreshing = isLoading)
            }
        }

        viewModelScope.launch {
            marketDataManager.errorMessage.collect { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            marketDataManager.refreshAllData()
        }
    }

    fun refreshRates() {
        Log.d(TAG, "Manual refresh rates triggered")
        viewModelScope.launch {
            marketDataManager.refreshAllData()

            // Analytics
            analyticsManager.logRatesRefreshed(
                success = _uiState.value.errorMessage == null
            )
        }
    }

    private fun updateCurrencyRates(rates: List<RateEntity>): List<RateDisplayModel> {
        val currencyNames = mapOf(
            "USD" to "Dolar",
            "EUR" to "Euro",
            "GBP" to "Sterlin"
        )

        val currencyIcons = mapOf(
            "USD" to Icons.Default.AttachMoney,
            "EUR" to Icons.Default.Euro,
            "GBP" to Icons.Default.CurrencyPound
        )

        val currencyColors = mapOf(
            "USD" to Color(0xFF4CAF50), // Green
            "EUR" to Color(0xFF2196F3), // Blue
            "GBP" to Color(0xFF9C27B0)  // Purple
        )

        // Sabit sıralama: Dolar, Euro, Sterlin
        val currencyOrder = listOf("USD", "EUR", "GBP")

        val rateMap = rates.associateBy { extractCurrencyCode(it.id) }

        return currencyOrder.mapNotNull { code ->
            val rate = rateMap[code] ?: return@mapNotNull null
            val title = currencyNames[code] ?: rate.name
            val icon = currencyIcons[code] ?: Icons.Default.CurrencyExchange
            val iconColor = currencyColors[code] ?: Color.Gray

            RateDisplayModel(
                id = rate.id,
                title = title,
                icon = icon,
                iconColor = iconColor,
                buyRate = formatPrice(rate.buyPrice),
                sellRate = formatPrice(rate.sellPrice),
                change = configureRateChangePercentage(rate.change),
                isChangeRatePositive = rate.isChangePercentPositive
            )
        }
    }

    private fun updateGoldRates(rates: List<RateEntity>): List<RateDisplayModel> {
        val goldNames = mapOf(
            "GOLD_GRAM" to "Gram Altın",
            "GOLD_QUARTER" to "Çeyrek Altın",
            "GOLD_HALF" to "Yarım Altın",
            "GOLD_FULL" to "Tam Altın",
            "GOLD_REPUBLIC" to "Cumhuriyet Altını",
            "GOLD_ATA" to "Ata Altın",
            "GOLD_BESLI" to "Beşli Altın",
            "GOLD_RESAT" to "Reşat Altın",
            "GOLD_HAMIT" to "Hamit Altın"
        )

        // Sabit sıralama: istenen sırayla
        val goldOrder = listOf(
            "GOLD_GRAM", "GOLD_QUARTER", "GOLD_HALF", "GOLD_FULL",
            "GOLD_REPUBLIC", "GOLD_ATA", "GOLD_BESLI", "GOLD_RESAT", "GOLD_HAMIT"
        )

        val rateMap = rates.associateBy { it.id }

        return goldOrder.mapNotNull { id ->
            val rate = rateMap[id] ?: return@mapNotNull null
            val title = goldNames[id] ?: rate.name

            RateDisplayModel(
                id = rate.id,
                title = title,
                icon = Icons.Default.Hive,
                iconColor = Color(0xFFFFD700),
                buyRate = formatPrice(rate.buyPrice),
                sellRate = formatPrice(rate.sellPrice),
                change = configureRateChangePercentage(rate.change),
                isChangeRatePositive = rate.isChangePercentPositive
            )
        }
    }

    private fun extractCurrencyCode(id: String): String {
        return when (id) {
            "USD" -> "USD"
            "EUR" -> "EUR"
            "GBP" -> "GBP"
            else -> ""
        }
    }

    private fun formatPrice(price: Double): String {
        return when {
            price >= 1000 -> {
                // 1000+ için: 2.850,50 formatı
                String.format("%,.2f", price)
                    .replace(",", "TEMP") // Geçici placeholder
                    .replace(".", ",")    // Ondalık ayracını virgül yap
                    .replace("TEMP", ".") // Binlik ayracını nokta yap
            }
            else -> {
                // 1000'den az için: 850,50 formatı
                String.format("%.2f", price).replace(".", ",")
            }
        }
    }

    private fun configureRateChangePercentage(changePercent: Double): String {
        return String.format("%.2f", kotlin.math.abs(changePercent))
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        marketDataManager.clearError()
    }
}