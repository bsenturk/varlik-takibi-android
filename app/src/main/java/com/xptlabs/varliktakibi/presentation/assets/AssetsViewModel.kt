package com.xptlabs.varliktakibi.presentation.assets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.managers.MarketDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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

                        if (portfolioData.profitLoss != 0.0) {
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
                assetRepository.insertAsset(asset)

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

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating asset: ${asset.name}")
                val existingAsset = assetRepository.getAssetById(asset.id)
                assetRepository.updateAsset(asset)

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

    private data class PortfolioData(
        val totalValue: Double = 0.0,
        val totalInvestment: Double = 0.0,
        val profitLoss: Double = 0.0,
        val profitLossPercentage: Double = 0.0
    )
}