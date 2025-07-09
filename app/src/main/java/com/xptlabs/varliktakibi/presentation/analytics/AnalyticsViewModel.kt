package com.xptlabs.varliktakibi.presentation.analytics

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.xptlabs.varliktakibi.BuildConfig
import kotlin.math.abs

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val totalValue: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val profitLoss: Double = 0.0,
    val profitLossPercentage: Double = 0.0,
    val hasProfitLossData: Boolean = false,
    val assetDistributions: List<AssetDistribution> = emptyList(),
    val errorMessage: String? = null
)

data class AssetDistribution(
    val name: String,
    val value: Double,
    val percentage: Double,
    val color: Color
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val analyticsManager: FirebaseAnalyticsManager
) : ViewModel() {

    companion object {
        private const val TAG = "AnalyticsViewModel"
    }

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "AnalyticsViewModel initialized")
        observeAssets()
    }

    private fun observeAssets() {
        viewModelScope.launch {
            assetRepository.getAllAssets()
                .catch { exception ->
                    Log.e(TAG, "Error observing assets", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message
                    )
                }
                .collect { assets ->
                    Log.d(TAG, "Assets updated: ${assets.size} assets")
                    calculateAnalytics(assets)
                }
        }
    }

    fun loadAnalytics() {
        Log.d(TAG, "Loading analytics")
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Analytics tracking
        analyticsManager.logCustomEvent(
            eventName = "analytics_viewed",
            parameters = emptyMap()
        )

        // DEBUG: Add test data if no real assets exist
        if (BuildConfig.DEBUG) {
            addTestDataIfNeeded()
        }
    }

    private fun addTestDataIfNeeded() {
        viewModelScope.launch {
            try {
                val currentAssets = assetRepository.getAllAssets().first()

                if (currentAssets.isEmpty()) {
                    Log.d(TAG, "No assets found, adding test data for analytics demo")
                    addTestAssets()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for test data", e)
            }
        }
    }

    private suspend fun addTestAssets() {
        try {
            val testAssets = listOf(
                // Test Gold Assets - Show profit
                createTestAsset(
                    name = "Test Gram Altın",
                    type = com.xptlabs.varliktakibi.domain.models.AssetType.GOLD,
                    amount = 10.0,
                    purchasePrice = 2500.0
                ),

                // Test USD - Show loss
                createTestAsset(
                    name = "Test Dolar",
                    type = com.xptlabs.varliktakibi.domain.models.AssetType.USD,
                    amount = 1000.0,
                    purchasePrice = 35.0   // Current 32.5 TL - LOSS
                ),

                // Test EUR - Show profit
                createTestAsset(
                    name = "Test Euro",
                    type = com.xptlabs.varliktakibi.domain.models.AssetType.EUR,
                    amount = 500.0,
                    purchasePrice = 36.0
                ),

                // Test Quarter Gold - Small amount
                createTestAsset(
                    name = "Test Çeyrek Altın",
                    type = com.xptlabs.varliktakibi.domain.models.AssetType.GOLD_QUARTER,
                    amount = 5.0,
                    purchasePrice = 720.0
                )
            )

            // Insert test assets
            testAssets.forEach { asset ->
                assetRepository.insertAsset(asset)
                Log.d(TAG, "Added test asset: ${asset.name} - Investment: ${asset.totalInvestment}, Current: ${asset.totalValue}")
            }

            Log.d(TAG, "Test assets added successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error adding test assets", e)
        }
    }

    private fun createTestAsset(
        name: String,
        type: AssetType,
        amount: Double,
        purchasePrice: Double
    ): Asset {
        return Asset(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            name = name,
            amount = amount,
            unit = type.unit,
            purchasePrice = purchasePrice,
            currentPrice = getCurrentPrice(type), // Use current market price
            dateAdded = java.util.Date(),
            lastUpdated = java.util.Date()
        )
    }

    private fun getCurrentPrice(assetType: AssetType): Double {
        // Use default prices as fallback for test data
        return when (assetType) {
            AssetType.GOLD -> 2850.0
            AssetType.GOLD_QUARTER -> 750.0
            AssetType.GOLD_HALF -> 1480.0
            AssetType.GOLD_FULL -> 2961.0
            AssetType.GOLD_REPUBLIC -> 3150.0
            AssetType.GOLD_ATA -> 3200.0
            AssetType.GOLD_RESAT -> 3180.0
            AssetType.GOLD_HAMIT -> 3175.0
            AssetType.USD -> 34.5
            AssetType.EUR -> 38.0
            AssetType.GBP -> 43.0
            AssetType.TRY -> 1.0
        }
    }

    private fun calculateAnalytics(assets: List<Asset>) {
        Log.d(TAG, "Calculating analytics for ${assets.size} assets")

        if (assets.isEmpty()) {
            _uiState.value = AnalyticsUiState(
                isLoading = false,
                assetDistributions = emptyList()
            )
            return
        }

        try {
            // Calculate totals
            val totalValue = assets.sumOf { it.totalValue }
            val totalInvestment = assets.sumOf { it.totalInvestment }
            val profitLoss = totalValue - totalInvestment
            val profitLossPercentage = if (totalInvestment > 0) {
                (profitLoss / totalInvestment) * 100
            } else 0.0

            val hasProfitLossData = totalInvestment > 0 &&
                    totalValue > 0 &&
                    abs(profitLoss) > 0.01

            // Calculate asset distributions
            val distributions = calculateAssetDistributions(assets, totalValue)

            Log.d(TAG, "Analytics calculated - Total Value: $totalValue, Investment: $totalInvestment, P/L: $profitLoss")

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                totalValue = totalValue,
                totalInvestment = totalInvestment,
                profitLoss = profitLoss,
                profitLossPercentage = profitLossPercentage,
                hasProfitLossData = hasProfitLossData,
                assetDistributions = distributions,
                errorMessage = null
            )

            // Analytics tracking
            analyticsManager.logPortfolioViewed(
                totalValue = totalValue,
                assetCount = assets.size
            )

            if (hasProfitLossData) {
                analyticsManager.logPortfolioProfitLoss(
                    profitLoss = profitLoss,
                    profitLossPercentage = profitLossPercentage
                )
            }

        } catch (exception: Exception) {
            Log.e(TAG, "Error calculating analytics", exception)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Analiz hesaplanırken hata oluştu: ${exception.message}"
            )

            analyticsManager.logError(
                errorType = "analytics_calculation_failed",
                errorMessage = exception.message ?: "Unknown error"
            )
        }
    }

    private fun calculateAssetDistributions(
        assets: List<Asset>,
        totalValue: Double
    ): List<AssetDistribution> {
        if (totalValue <= 0) {
            Log.w(TAG, "Total portfolio value is zero or negative: $totalValue")
            return emptyList()
        }

        // Group assets by type
        val groupedAssets = assets.groupBy { it.type }

        Log.d(TAG, "Calculating asset distribution:")
        Log.d(TAG, "Total portfolio value: $totalValue")
        Log.d(TAG, "Number of asset groups: ${groupedAssets.size}")

        val distributions = mutableListOf<AssetDistribution>()

        // Calculate for each asset type
        groupedAssets.forEach { (type, assetsOfType) ->
            val totalValueForType = assetsOfType.sumOf { asset ->
                val assetValue = asset.totalValue
                Log.d(TAG, "Asset: ${asset.name}, Amount: ${asset.amount}, Price: ${asset.currentPrice}, Total: $assetValue")
                assetValue
            }

            // Skip if value is zero or negative
            if (totalValueForType <= 0) {
                Log.w(TAG, "Skipping ${type.displayName} - zero or negative value: $totalValueForType")
                return@forEach
            }

            // Calculate exact percentage with double precision
            val exactPercentage = (totalValueForType / totalValue) * 100.0

            Log.d(TAG, "${type.displayName}: Value=$totalValueForType, TotalValue=$totalValue, ExactPercentage=$exactPercentage%")

            val distribution = AssetDistribution(
                name = type.displayName,
                value = totalValueForType,
                percentage = exactPercentage,
                color = getAssetColor(type)
            )

            distributions.add(distribution)
        }

        // Sort by value (largest to smallest)
        distributions.sortByDescending { it.value }

        // Validation - check total percentage
        val totalCalculatedPercentage = distributions.sumOf { it.percentage }
        Log.d(TAG, "Total calculated percentage: $totalCalculatedPercentage%")

        // Normalize if needed (for precision errors)
        if (abs(totalCalculatedPercentage - 100.0) > 0.01 && distributions.isNotEmpty()) {
            Log.d(TAG, "Normalizing percentages to total 100%...")

            val normalizationFactor = 100.0 / totalCalculatedPercentage
            val normalizedDistributions = distributions.map { distribution ->
                val normalizedPercentage = distribution.percentage * normalizationFactor
                Log.d(TAG, "${distribution.name}: ${distribution.percentage}% -> $normalizedPercentage%")
                distribution.copy(percentage = normalizedPercentage)
            }

            val finalTotal = normalizedDistributions.sumOf { it.percentage }
            Log.d(TAG, "Final total percentage: $finalTotal%")

            return normalizedDistributions
        }

        return distributions
    }

    private fun getAssetColor(assetType: AssetType): Color {
        return when (assetType) {
            AssetType.GOLD,
            AssetType.GOLD_QUARTER,
            AssetType.GOLD_HALF,
            AssetType.GOLD_FULL,
            AssetType.GOLD_REPUBLIC,
            AssetType.GOLD_ATA,
            AssetType.GOLD_RESAT,
            AssetType.GOLD_HAMIT -> Color(0xFFFFD700) // Gold

            AssetType.USD -> Color(0xFF4CAF50) // Green
            AssetType.EUR -> Color(0xFF2196F3) // Blue
            AssetType.GBP -> Color(0xFF9C27B0) // Purple
            AssetType.TRY -> Color(0xFFF44336) // Red
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun generateRandomTestData() {
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Generating random test data from Analytics")

                    // Clear existing assets first
                    assetRepository.deleteAllAssets()

                    // Generate random scenarios with different profit/loss outcomes
                    val scenarios = listOf(
                        // Scenario 1: Strong profit scenario
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 20.0, 2400.0),
                            createTestAsset("Dolar", AssetType.USD, 2500.0, 32.0),
                            createTestAsset("Euro", AssetType.EUR, 1000.0, 35.0),
                            createTestAsset("Çeyrek Altın", AssetType.GOLD_QUARTER, 3.0, 700.0)
                        ),
                        // Scenario 2: Loss scenario
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 12.0, 2950.0),
                            createTestAsset("Dolar", AssetType.USD, 1800.0, 36.0),
                            createTestAsset("Euro", AssetType.EUR, 800.0, 40.0),
                            createTestAsset("Sterlin", AssetType.GBP, 200.0, 45.0)
                        ),
                        // Scenario 3: Mixed with heavy diversification
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 15.0, 2700.0),
                            createTestAsset("Dolar", AssetType.USD, 3500.0, 33.5),
                            createTestAsset("Euro", AssetType.EUR, 1500.0, 36.5),
                            createTestAsset("Sterlin", AssetType.GBP, 500.0, 41.0),
                            createTestAsset("Çeyrek Altın", AssetType.GOLD_QUARTER, 2.0, 750.0),
                            createTestAsset("Yarım Altın", AssetType.GOLD_HALF, 1.0, 1450.0)
                        ),
                        // Scenario 4: Extreme profit scenario
                        listOf(
                            createTestAsset("Gram Altın", AssetType.GOLD, 25.0, 2200.0),
                            createTestAsset("Dolar", AssetType.USD, 5000.0, 30.0),
                            createTestAsset("Euro", AssetType.EUR, 2000.0, 34.0)
                        )
                    )

                    // Select random scenario
                    val selectedScenario = scenarios.random()

                    // Add assets to repository
                    selectedScenario.forEach { asset ->
                        assetRepository.insertAsset(asset)
                    }

                    Log.d(TAG, "Random test data generated: ${selectedScenario.size} assets")

                    // Analytics tracking
                    analyticsManager.logCustomEvent(
                        eventName = "debug_analytics_test_data_generated",
                        parameters = mapOf(
                            "asset_count" to selectedScenario.size,
                            "scenario" to "analytics_random"
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
                    Log.d(TAG, "Clearing all test data from Analytics")
                    assetRepository.deleteAllAssets()

                    // Analytics tracking
                    analyticsManager.logCustomEvent(
                        eventName = "debug_analytics_test_data_cleared",
                        parameters = emptyMap()
                    )

                    Log.d(TAG, "All test data cleared from Analytics")

                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing test data", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Test verisi temizlenemedi: ${e.message}"
                    )
                }
            }
        }
    }
}