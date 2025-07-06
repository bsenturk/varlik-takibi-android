package com.xptlabs.varliktakibi.managers

import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.domain.models.Rate
import com.xptlabs.varliktakibi.domain.repository.RateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDataManager @Inject constructor(
    private val rateRepository: RateRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _goldPrices = MutableStateFlow<List<Rate>>(emptyList())
    val goldPrices: StateFlow<List<Rate>> = _goldPrices.asStateFlow()

    private val _currencyRates = MutableStateFlow<List<Rate>>(emptyList())
    val currencyRates: StateFlow<List<Rate>> = _currencyRates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow<Date?>(null)
    val lastUpdateTime: StateFlow<Date?> = _lastUpdateTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Repository'den gelen verileri dinle
        setupBindings()
        // İlk veriler için hemen fetch et
        scope.launch {
            refreshAllData()
        }
    }

    private fun setupBindings() {
        // Repository'den gold rates'i dinle
        scope.launch {
            rateRepository.getLastGoldRates().collect { rates ->
                _goldPrices.value = rates
            }
        }

        // Repository'den currency rates'i dinle
        scope.launch {
            rateRepository.getLastCurrencyRates().collect { rates ->
                _currencyRates.value = rates
            }
        }
    }

    suspend fun refreshAllData() {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val result = rateRepository.refreshAllRates()

            result.onSuccess { (goldRates, currencyRates) ->
                _lastUpdateTime.value = Date()
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Veri güncellenirken hata oluştu"
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Bilinmeyen hata"
        } finally {
            _isLoading.value = false
        }
    }

    fun getGoldPrice(assetType: AssetType): Rate? {
        return when (assetType) {
            AssetType.GOLD -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("gram altın")
            }
            AssetType.GOLD_QUARTER -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("çeyrek")
            }
            AssetType.GOLD_HALF -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("yarım")
            }
            AssetType.GOLD_FULL -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("tam altın")
            }
            AssetType.GOLD_REPUBLIC -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("cumhuriyet")
            }
            AssetType.GOLD_ATA -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("ata")
            }
            AssetType.GOLD_RESAT -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("reşat")
            }
            AssetType.GOLD_HAMIT -> _goldPrices.value.firstOrNull {
                it.name.lowercase().contains("hamit")
            }
            else -> null
        }
    }

    fun getCurrencyRate(assetType: AssetType): Rate? {
        return when (assetType) {
            AssetType.USD -> _currencyRates.value.firstOrNull {
                it.code?.uppercase() == "USD"
            }
            AssetType.EUR -> _currencyRates.value.firstOrNull {
                it.code?.uppercase() == "EUR"
            }
            AssetType.GBP -> _currencyRates.value.firstOrNull {
                it.code?.uppercase() == "GBP"
            }
            AssetType.TRY -> Rate(
                name = "Türk Lirası",
                code = "TRY",
                buyPrice = 1.0,
                sellPrice = 1.0,
                change = 0.0,
                changePercent = 0.0
            )
            else -> null
        }
    }

    fun getCurrentPrice(assetType: AssetType): Double {
        return when {
            isGoldAsset(assetType) -> getGoldPrice(assetType)?.sellPrice ?: getDefaultPrice(assetType)
            isCurrencyAsset(assetType) -> getCurrencyRate(assetType)?.sellPrice ?: getDefaultPrice(assetType)
            else -> getDefaultPrice(assetType)
        }
    }

    // Asset türü kontrolü
    private fun isGoldAsset(assetType: AssetType): Boolean {
        return when (assetType) {
            AssetType.GOLD, AssetType.GOLD_QUARTER, AssetType.GOLD_HALF,
            AssetType.GOLD_FULL, AssetType.GOLD_REPUBLIC, AssetType.GOLD_ATA,
            AssetType.GOLD_RESAT, AssetType.GOLD_HAMIT -> true
            else -> false
        }
    }

    private fun isCurrencyAsset(assetType: AssetType): Boolean {
        return when (assetType) {
            AssetType.USD, AssetType.EUR, AssetType.GBP, AssetType.TRY -> true
            else -> false
        }
    }

    private fun getDefaultPrice(assetType: AssetType): Double {
        return when (assetType) {
            AssetType.GOLD -> 2850.50
            AssetType.GOLD_QUARTER -> 740.25
            AssetType.GOLD_HALF -> 1480.50
            AssetType.GOLD_FULL -> 2961.00
            AssetType.GOLD_REPUBLIC -> 3150.75
            AssetType.GOLD_ATA -> 3200.00
            AssetType.GOLD_RESAT -> 3180.25
            AssetType.GOLD_HAMIT -> 3175.50
            AssetType.USD -> 34.85
            AssetType.EUR -> 36.42
            AssetType.GBP -> 43.15
            AssetType.TRY -> 1.0
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: MarketDataManager? = null

        fun getInstance(rateRepository: RateRepository): MarketDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketDataManager(rateRepository).also { INSTANCE = it }
            }
        }
    }
}