package com.xptlabs.varliktakibi.presentation.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetsUiState(
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalPortfolioValue: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val profitLoss: Double = 0.0,
    val profitLossPercentage: Double = 0.0
)

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val analyticsManager: FirebaseAnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        observeAssets()
    }

    private fun observeAssets() {
        viewModelScope.launch {
            assetRepository.getAllAssets()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message,
                        isLoading = false
                    )
                }
                .collect { assets ->
                    val portfolioData = calculatePortfolioData(assets)
                    _uiState.value = _uiState.value.copy(
                        assets = assets,
                        isLoading = false,
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

    fun loadAssets() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // Assets are already being observed, this just shows loading state
    }

    fun addAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                assetRepository.insertAsset(asset)

                // Analytics
                analyticsManager.logAssetAdded(
                    assetType = asset.type.name,
                    amount = asset.amount
                )

            } catch (exception: Exception) {
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
                assetRepository.deleteAsset(asset)

                // Analytics
                analyticsManager.logAssetDeleted(
                    assetType = asset.type.name,
                    amount = asset.amount
                )

            } catch (exception: Exception) {
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