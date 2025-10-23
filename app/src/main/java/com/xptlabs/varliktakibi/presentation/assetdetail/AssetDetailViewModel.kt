package com.xptlabs.varliktakibi.presentation.assetdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.managers.AssetHistoryManager
import com.xptlabs.varliktakibi.managers.MarketDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ChartDataPoint(
    val timestamp: Long,
    val value: Double
)

data class AssetDetailUiState(
    val asset: Asset? = null,
    val priceHistory: List<AssetPriceHistoryEntity> = emptyList(),
    val transactionHistory: List<AssetTransactionHistoryEntity> = emptyList(),
    val selectedChartPeriod: ChartPeriod = ChartPeriod.WEEKLY,
    val chartData: List<ChartDataPoint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val historyManager: AssetHistoryManager,
    private val marketDataManager: MarketDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetDetailUiState())
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

    fun loadAssetDetail(assetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load asset (use first() to get single value, not collect)
                val assets = assetRepository.getAllAssets().first()
                val asset = assets.find { it.id == assetId }

                if (asset != null) {
                    // Update with current market price
                    val currentPrice = marketDataManager.getCurrentPrice(asset.type)
                    val updatedAsset = asset.copy(
                        currentPrice = currentPrice,
                        lastUpdated = Date()
                    )

                    Log.d("AssetDetailViewModel", "Updated ${asset.name}: old price=${asset.currentPrice}, new price=$currentPrice")

                    // Save updated asset with current price to repository
                    assetRepository.updateAsset(updatedAsset)

                    // Record daily snapshot (updates if same day, creates new if different day)
                    historyManager.recordDailySnapshot(updatedAsset)

                    // Load price history (last 30 days)
                    val priceHistory = historyManager.getPriceHistory(assetId, 30)

                    // Load transaction history (max 10 recent)
                    val transactionHistory = historyManager.getRecentTransactions(assetId, 10)

                    _uiState.value = _uiState.value.copy(
                        asset = updatedAsset,  // Use updated asset with current price
                        priceHistory = priceHistory,
                        transactionHistory = transactionHistory,
                        isLoading = false
                    )

                    // Update chart data with initial period
                    updateChartData(_uiState.value.selectedChartPeriod)

                    Log.d("AssetDetailViewModel", "Loaded asset: ${updatedAsset.name}")
                    Log.d("AssetDetailViewModel", "Price history records: ${priceHistory.size}")
                    Log.d("AssetDetailViewModel", "Transaction history records: ${transactionHistory.size}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Varlık bulunamadı"
                    )
                }
            } catch (e: Exception) {
                Log.e("AssetDetailViewModel", "Error loading asset detail", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Varlık detayları yüklenirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    fun setChartPeriod(period: ChartPeriod) {
        _uiState.value = _uiState.value.copy(selectedChartPeriod = period)
        updateChartData(period)
        Log.d("AssetDetailViewModel", "Chart period changed to: ${period.label}")
    }

    private fun updateChartData(period: ChartPeriod) {
        val priceHistory = _uiState.value.priceHistory
        if (priceHistory.isEmpty()) return

        val now = System.currentTimeMillis()
        val cutoffTime = when (period) {
            ChartPeriod.DAILY -> now - TimeUnit.DAYS.toMillis(1L)
            ChartPeriod.WEEKLY -> now - TimeUnit.DAYS.toMillis(7L)
            ChartPeriod.MONTHLY -> now - TimeUnit.DAYS.toMillis(30L)
        }

        val filteredData = priceHistory
            .filter { it.date.time >= cutoffTime }
            .sortedBy { it.date.time }
            .map { ChartDataPoint(it.date.time, it.totalValue) }

        _uiState.value = _uiState.value.copy(chartData = filteredData)
        Log.d("AssetDetailViewModel", "Chart data updated: ${filteredData.size} points")
    }
}
