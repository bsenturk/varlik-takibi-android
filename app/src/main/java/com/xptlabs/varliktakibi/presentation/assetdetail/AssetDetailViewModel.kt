package com.xptlabs.varliktakibi.presentation.assetdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.managers.AssetHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetDetailUiState(
    val asset: Asset? = null,
    val priceHistory: List<AssetPriceHistoryEntity> = emptyList(),
    val transactionHistory: List<AssetTransactionHistoryEntity> = emptyList(),
    val selectedChartPeriod: ChartPeriod = ChartPeriod.WEEKLY,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val historyManager: AssetHistoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetDetailUiState())
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

    fun loadAssetDetail(assetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load asset
                assetRepository.getAllAssets().collect { assets ->
                    val asset = assets.find { it.id == assetId }

                    if (asset != null) {
                        // Load price history
                        val priceHistory = historyManager.getPriceHistory(assetId)

                        // Load transaction history
                        val transactionHistory = historyManager.getTransactionHistory(assetId)

                        _uiState.value = _uiState.value.copy(
                            asset = asset,
                            priceHistory = priceHistory,
                            transactionHistory = transactionHistory,
                            isLoading = false
                        )

                        Log.d("AssetDetailViewModel", "Loaded asset: ${asset.name}")
                        Log.d("AssetDetailViewModel", "Price history records: ${priceHistory.size}")
                        Log.d("AssetDetailViewModel", "Transaction history records: ${transactionHistory.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Varlık bulunamadı"
                        )
                    }
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
        Log.d("AssetDetailViewModel", "Chart period changed to: ${period.label}")
    }
}
