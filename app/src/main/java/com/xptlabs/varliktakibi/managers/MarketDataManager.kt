package com.xptlabs.varliktakibi.managers

import android.util.Log
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.domain.models.AssetType
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
    companion object {
        private const val TAG = "MarketDataManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _goldRates = MutableStateFlow<List<RateEntity>>(emptyList())
    val goldRates: StateFlow<List<RateEntity>> = _goldRates.asStateFlow()

    private val _currencyRates = MutableStateFlow<List<RateEntity>>(emptyList())
    val currencyRates: StateFlow<List<RateEntity>> = _currencyRates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow<Date?>(null)
    val lastUpdateTime: StateFlow<Date?> = _lastUpdateTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        Log.d(TAG, "MarketDataManager initialized")
        setupBindings()
        // İlk veri yükleme
        scope.launch {
            refreshAllData()
        }
    }

    private fun setupBindings() {
        // Repository'den gold rates'i dinle
        scope.launch {
            rateRepository.getGoldRates().collect { rates ->
                Log.d(TAG, "Received ${rates.size} gold rates from database")
                _goldRates.value = rates
            }
        }

        // Repository'den currency rates'i dinle
        scope.launch {
            rateRepository.getCurrencyRates().collect { rates ->
                Log.d(TAG, "Received ${rates.size} currency rates from database")
                _currencyRates.value = rates
            }
        }
    }

    suspend fun refreshAllData() {
        Log.d(TAG, "Starting refresh all data")
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val result = rateRepository.refreshAllRates()

            result.onSuccess { (goldRates, currencyRates) ->
                Log.d(TAG, "Refresh successful: ${goldRates.size} gold, ${currencyRates.size} currency")
                _lastUpdateTime.value = Date()
                _errorMessage.value = null
            }.onFailure { error ->
                Log.e(TAG, "Refresh failed: ${error.message}", error)
                _errorMessage.value = error.message ?: "Veri güncellenirken hata oluştu"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh exception: ${e.message}", e)
            _errorMessage.value = e.message ?: "Bilinmeyen hata"
        } finally {
            _isLoading.value = false
        }
    }

    fun getCurrentPrice(assetType: AssetType): Double {
        return when {
            isGoldAsset(assetType) -> {
                val rate = getGoldRate(assetType)
                val price = rate?.sellPrice ?: getDefaultPrice(assetType)
                Log.d(TAG, "Gold price for $assetType: $price")
                price
            }
            isCurrencyAsset(assetType) -> {
                val rate = getCurrencyRate(assetType)
                val price = rate?.sellPrice ?: getDefaultPrice(assetType)
                Log.d(TAG, "Currency price for $assetType: $price")
                price
            }
            else -> {
                val price = getDefaultPrice(assetType)
                Log.d(TAG, "Default price for $assetType: $price")
                price
            }
        }
    }

    private fun getGoldRate(assetType: AssetType): RateEntity? {
        val goldRates = _goldRates.value
        Log.d(TAG, "Looking for gold rate in ${goldRates.size} rates for $assetType")

        val goldId = when (assetType) {
            AssetType.GOLD -> "GOLD_GRAM"
            AssetType.GOLD_QUARTER -> "GOLD_QUARTER"
            AssetType.GOLD_HALF -> "GOLD_HALF"
            AssetType.GOLD_FULL -> "GOLD_FULL"
            AssetType.GOLD_REPUBLIC -> "GOLD_REPUBLIC"
            AssetType.GOLD_ATA -> "GOLD_ATA"
            AssetType.GOLD_RESAT -> "GOLD_RESAT"
            AssetType.GOLD_HAMIT -> "GOLD_HAMIT"
            else -> return null
        }

        return goldRates.firstOrNull { it.id == goldId }
    }

    private fun getCurrencyRate(assetType: AssetType): RateEntity? {
        val currencyRates = _currencyRates.value
        Log.d(TAG, "Looking for currency rate in ${currencyRates.size} rates for $assetType")

        val currencyId = when (assetType) {
            AssetType.USD -> "USD"
            AssetType.EUR -> "EUR"
            AssetType.GBP -> "GBP"
            AssetType.TRY -> return RateEntity(
                id = "TRY",
                name = "Türk Lirası",
                type = "CURRENCY",
                buyPrice = 1.0,
                sellPrice = 1.0,
                change = 0.0,
                changePercent = 0.0,
                lastUpdated = Date(),
                isChangePercentPositive = true
            )
            else -> return null
        }

        return currencyRates.firstOrNull { it.id == currencyId }
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

    // Güncel kur bilgisi için
    fun getCurrentRate(assetType: AssetType): RateEntity? {
        return when {
            isGoldAsset(assetType) -> getGoldRate(assetType)
            isCurrencyAsset(assetType) -> getCurrencyRate(assetType)
            else -> null
        }
    }
}