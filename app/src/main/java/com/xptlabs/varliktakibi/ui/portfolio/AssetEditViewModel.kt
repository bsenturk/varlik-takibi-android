package com.xptlabs.varliktakibi.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.category
import com.xptlabs.varliktakibi.data.repo.AssetEditor
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.market.MarketDataStore
import com.xptlabs.varliktakibi.ui.addasset.toDoubleOrNullTr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetEditUiState(
    val asset: AssetEntity? = null,
    val marketPrice: Double? = null,
    val errorMessage: String? = null,
    val finished: Boolean = false
)

@HiltViewModel
class AssetEditViewModel @Inject constructor(
    private val assetDao: AssetDao,
    private val editor: AssetEditor,
    private val repository: PortfolioRepository,
    private val market: MarketDataStore,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetEditUiState())
    val uiState: StateFlow<AssetEditUiState> = _uiState.asStateFlow()

    fun load(assetId: String) = viewModelScope.launch {
        val asset = assetDao.getById(assetId)
        _uiState.update {
            it.copy(asset = asset, marketPrice = asset?.let { a -> market.tryPrice(a.symbol) })
        }
    }

    fun save(amountText: String, costText: String) {
        val asset = _uiState.value.asset ?: return
        val amount = amountText.toDoubleOrNullTr()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Lütfen geçerli bir miktar girin.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                costText.toDoubleOrNullTr()?.takeIf { it > 0 }
                    ?.let { editor.setCostBasis(asset, it) }
                // Maliyet güncellendiyse en taze satırla devam et.
                val fresh = assetDao.getById(asset.id) ?: asset
                editor.setAmount(fresh, amount)
            }.onSuccess {
                _uiState.update { it.copy(finished = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Varlık güncellenemedi.")
                }
            }
        }
    }

    fun delete() {
        val asset = _uiState.value.asset ?: return
        viewModelScope.launch {
            repository.deleteAsset(asset)
            analytics.logAssetDeleted(asset.category.name, asset.symbol)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
