package com.xptlabs.varliktakibi.presentation.assets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.managers.MarketDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

data class AssetsUiState(
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val totalPortfolioValue: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val profitLoss: Double = 0.0,
    val profitLossPercentage: Double = 0.0,
    val hasDataLoaded: Boolean = false
)

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val analyticsManager: FirebaseAnalyticsManager,
    val marketDataManager: MarketDataManager
) : ViewModel() {

    companion object {
        private const val TAG = "AssetsViewModel"
    }

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "AssetsViewModel initialized")
        observeAssets()
        observeMarketData()
        loadInitialData()
    }

    private fun loadInitialData() {
        Log.d(TAG, "Loading initial data")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Market verilerini yenile
                Log.d(TAG, "Refreshing market data")
                marketDataManager.refreshAllData()

                Log.d(TAG, "Initial data load completed")

            } catch (exception: Exception) {
                Log.e(TAG, "Initial data load failed", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Veriler yüklenirken hata: ${exception.message}"
                )
            }
        }
    }

    private fun observeAssets() {
        viewModelScope.launch {
            assetRepository.getAllAssets()
                .catch { exception ->
                    Log.e(TAG, "Error observing assets", exception)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message,
                        isLoading = false
                    )
                }
                .collect { assets ->
                    Log.d(TAG, "Assets updated: ${assets.size} assets")

                    // Update asset prices with current market data
                    val portfolioData = calculatePortfolioData(assets)

                    _uiState.value = _uiState.value.copy(
                        assets = assets,
                        isLoading = false,
                        hasDataLoaded = true,
                        errorMessage = null,
                        totalPortfolioValue = portfolioData.totalValue,
                        totalInvestment = portfolioData.totalInvestment,
                        profitLoss = portfolioData.profitLoss,
                        profitLossPercentage = portfolioData.profitLossPercentage
                    )

                    // Analytics
                    if (assets.isNotEmpty()) {
                        analyticsManager.logPortfolioViewed(
                            totalValue = portfolioData.totalValue,
                            assetCount = assets.size
                        )

                        if (abs(portfolioData.profitLoss) > 0.01) {
                            analyticsManager.logPortfolioProfitLoss(
                                profitLoss = portfolioData.profitLoss,
                                profitLossPercentage = portfolioData.profitLossPercentage
                            )
                        }
                    }
                }
        }
    }

    private fun observeMarketData() {
        viewModelScope.launch {
            marketDataManager.isLoading.collect { isLoading ->
                Log.d(TAG, "Market data loading state: $isLoading")
                _uiState.value = _uiState.value.copy(
                    isRefreshing = isLoading,
                    isLoading = if (_uiState.value.hasDataLoaded) false else isLoading
                )
            }
        }

        viewModelScope.launch {
            marketDataManager.errorMessage.collect { error ->
                if (error != null) {
                    Log.e(TAG, "Market data error: $error")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error,
                        isLoading = false
                    )
                }
            }
        }

        // Market data changes should trigger asset price updates
        viewModelScope.launch {
            combine(
                marketDataManager.goldRates,
                marketDataManager.currencyRates
            ) { _, _ ->
                // Trigger asset price update when market data changes
                Log.d(TAG, "Market data updated, refreshing asset prices")
                Unit
            }.collect {
                // This will trigger the assets observer which will update prices
            }
        }
    }

    private fun updateAssetPrices(assets: List<Asset>): List<Asset> {
        return assets.map { asset ->
            val currentPrice = marketDataManager.getCurrentPrice(asset.type)
            asset.copy(
                currentPrice = currentPrice,
                lastUpdated = Date()
            )
        }
    }

    fun loadAssets() {
        Log.d(TAG, "Manual load assets triggered")
        if (!_uiState.value.hasDataLoaded) {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }
        // Assets are already being observed, just refresh market data
        viewModelScope.launch {
            marketDataManager.refreshAllData()
        }
    }

    fun refreshData() {
        Log.d(TAG, "Refresh data triggered")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            marketDataManager.refreshAllData()
        }
    }

    fun addAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding asset: ${asset.name}")
                val updatedAsset = asset.copy(
                    currentPrice = marketDataManager.getCurrentPrice(asset.type),
                    lastUpdated = Date()
                )

                assetRepository.insertAsset(updatedAsset)

                // Analytics
                analyticsManager.logAssetAdded(
                    assetType = asset.type.name,
                    amount = asset.amount
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Error adding asset", exception)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Varlık eklenirken hata oluştu: ${exception.message}"
                )

                analyticsManager.logError(
                    errorType = "asset_add_failed",
                    errorMessage = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun addOrUpdateAsset(newAsset: Asset) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding or updating asset: ${newAsset.name} - ${newAsset.type}")

                // Get current price
                val currentPrice = marketDataManager.getCurrentPrice(newAsset.type)
                val updatedAsset = newAsset.copy(
                    currentPrice = currentPrice,
                    lastUpdated = Date()
                )

                // Check if asset of same type already exists
                val currentAssets = assetRepository.getAllAssets().first()
                val existingAsset = currentAssets.find { it.type == newAsset.type }

                if (existingAsset != null) {
                    // Update existing asset - add amounts
                    val combinedAsset = existingAsset.copy(
                        amount = existingAsset.amount + newAsset.amount,
                        // Calculate weighted average purchase price
                        purchasePrice = calculateWeightedAveragePrice(
                            existingAsset.amount, existingAsset.purchasePrice,
                            newAsset.amount, newAsset.purchasePrice
                        ),
                        currentPrice = currentPrice,
                        lastUpdated = Date()
                    )

                    Log.d(TAG, "Updating existing asset: ${existingAsset.amount} + ${newAsset.amount} = ${combinedAsset.amount}")
                    assetRepository.updateAsset(combinedAsset)

                    // Analytics
                    analyticsManager.logAssetUpdated(
                        assetType = combinedAsset.type.name,
                        oldAmount = existingAsset.amount,
                        newAmount = combinedAsset.amount
                    )
                } else {
                    // Add new asset
                    Log.d(TAG, "Adding new asset: ${updatedAsset.name}")
                    assetRepository.insertAsset(updatedAsset)

                    // Analytics
                    analyticsManager.logAssetAdded(
                        assetType = updatedAsset.type.name,
                        amount = updatedAsset.amount
                    )
                }

            } catch (exception: Exception) {
                Log.e(TAG, "Error adding/updating asset", exception)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Varlık eklenirken hata oluştu: ${exception.message}"
                )

                analyticsManager.logError(
                    errorType = "asset_add_update_failed",
                    errorMessage = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating asset: ${asset.name}")
                val existingAsset = assetRepository.getAssetById(asset.id)

                val updatedAsset = asset.copy(
                    currentPrice = marketDataManager.getCurrentPrice(asset.type),
                    lastUpdated = Date()
                )

                assetRepository.updateAsset(updatedAsset)

                // Analytics
                existingAsset?.let { existing ->
                    analyticsManager.logAssetUpdated(
                        assetType = asset.type.name,
                        oldAmount = existing.amount,
                        newAmount = asset.amount
                    )
                }

            } catch (exception: Exception) {
                Log.e(TAG, "Error updating asset", exception)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Varlık güncellenirken hata oluştu: ${exception.message}"
                )

                analyticsManager.logError(
                    errorType = "asset_update_failed",
                    errorMessage = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting asset: ${asset.name}")
                assetRepository.deleteAsset(asset)

                // Analytics
                analyticsManager.logAssetDeleted(
                    assetType = asset.type.name,
                    amount = asset.amount
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Error deleting asset", exception)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Varlık silinirken hata oluştu: ${exception.message}"
                )

                analyticsManager.logError(
                    errorType = "asset_delete_failed",
                    errorMessage = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        marketDataManager.clearError()
    }

    // Debug functions for testing
    fun generateRandomTestData() {
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Generating random test data")

                    // Clear existing assets first
                    assetRepository.deleteAllAssets()

                    // Generate random scenarios
                    val scenarios = listOf(
                        // Scenario 1: Overall profit
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 15.0, 2400.0),
                            createTestAsset("Dolar", AssetType.USD, 2000.0, 33.0),
                            createTestAsset("Euro", AssetType.EUR, 800.0, 35.5),
                            createTestAsset("Çeyrek Altın", AssetType.GOLD_QUARTER, 2.0, 720.0)
                        ),
                        // Scenario 2: Overall loss
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 8.0, 2900.0),
                            createTestAsset("Dolar", AssetType.USD, 1500.0, 35.5),
                            createTestAsset("Euro", AssetType.EUR, 600.0, 39.0),
                            createTestAsset("Yarım Altın", AssetType.GOLD_HALF, 1.0, 1550.0)
                        ),
                        // Scenario 3: Mixed results
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 12.0, 2600.0),
                            createTestAsset("Dolar", AssetType.USD, 3000.0, 34.0),
                            createTestAsset("Euro", AssetType.EUR, 1200.0, 37.0),
                            createTestAsset("Sterlin", AssetType.GBP, 300.0, 42.0),
                            createTestAsset("Tam Altın", AssetType.GOLD_FULL, 1.0, 2950.0)
                        )
                    )

                    // Select random scenario
                    val selectedScenario = scenarios.random()

                    // Add assets
                    selectedScenario.forEach { asset ->
                        assetRepository.insertAsset(asset)
                    }

                    Log.d(TAG, "Random test data generated: ${selectedScenario.size} assets")

                    // Analytics
                    analyticsManager.logCustomEvent(
                        eventName = "debug_test_data_generated",
                        parameters = mapOf(
                            "asset_count" to selectedScenario.size,
                            "scenario" to "random"
                        )
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Error generating random test data", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Test verisi oluşturulamadı: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearTestData() {
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Clearing all test data")
                    assetRepository.deleteAllAssets()

                    // Analytics
                    analyticsManager.logCustomEvent(
                        eventName = "debug_test_data_cleared",
                        parameters = emptyMap()
                    )

                    Log.d(TAG, "All test data cleared")

                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing test data", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Test verisi temizlenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    // Debug function - add test data if empty
    private fun addTestDataIfEmpty() {
        viewModelScope.launch {
            try {
                val currentAssets = assetRepository.getAllAssets().first()
                if (currentAssets.isEmpty()) {
                    Log.d(TAG, "No assets found, adding test data")

                    val testAssets = listOf(
                        createTestAsset(
                            name = "Gram Altın",
                            type = AssetType.GOLD,
                            amount = 10.0,
                            purchasePrice = 2600.0
                        ),
                        createTestAsset(
                            name = "Dolar",
                            type = AssetType.USD,
                            amount = 1000.0,
                            purchasePrice = 34.0
                        ),
                        createTestAsset(
                            name = "Euro",
                            type = AssetType.EUR,
                            amount = 500.0,
                            purchasePrice = 37.0
                        )
                    )

                    testAssets.forEach { asset ->
                        assetRepository.insertAsset(asset)
                    }

                    Log.d(TAG, "Test assets added: ${testAssets.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding test data", e)
            }
        }
    }

    private fun createTestAsset(
        name: String,
        type: AssetType,
        amount: Double,
        purchasePrice: Double
    ): Asset {
        val currentPrice = marketDataManager.getCurrentPrice(type)
        return Asset(
            id = UUID.randomUUID().toString(),
            type = type,
            name = name,
            amount = amount,
            unit = type.unit,
            purchasePrice = purchasePrice,
            currentPrice = currentPrice,
            dateAdded = Date(),
            lastUpdated = Date()
        )
    }

    private fun calculatePortfolioData(assets: List<Asset>): PortfolioData {
        if (assets.isEmpty()) {
            return PortfolioData()
        }

        val totalValue = assets.sumOf { it.totalValue }
        val totalInvestment = assets.sumOf { it.totalInvestment }
        val profitLoss = totalValue - totalInvestment
        val profitLossPercentage = if (totalInvestment > 0) {
            (profitLoss / totalInvestment) * 100
        } else 0.0

        Log.d(TAG, "Portfolio calculated - Value: $totalValue, Investment: $totalInvestment, P/L: $profitLoss")

        return PortfolioData(
            totalValue = totalValue,
            totalInvestment = totalInvestment,
            profitLoss = profitLoss,
            profitLossPercentage = profitLossPercentage
        )
    }

    private fun calculateWeightedAveragePrice(
        existingAmount: Double,
        existingPrice: Double,
        newAmount: Double,
        newPrice: Double
    ): Double {
        val totalAmount = existingAmount + newAmount
        return if (totalAmount > 0) {
            ((existingAmount * existingPrice) + (newAmount * newPrice)) / totalAmount
        } else {
            existingPrice
        }
    }

    private data class PortfolioData(
        val totalValue: Double = 0.0,
        val totalInvestment: Double = 0.0,
        val profitLoss: Double = 0.0,
        val profitLossPercentage: Double = 0.0
    )
}