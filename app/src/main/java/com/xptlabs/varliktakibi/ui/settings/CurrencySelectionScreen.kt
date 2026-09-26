package com.xptlabs.varliktakibi.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.market.MarketDataStore
import com.xptlabs.varliktakibi.ui.common.ScreenNavBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencySelectionViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val market: MarketDataStore,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    val selected: StateFlow<Currency> = prefs.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.TRY)

    /** "USD · 1 USD = 48,43 ₺". Kur henüz yoksa yalnızca kod — yanıltıcı sıfır yerine. */
    fun subtitle(currency: Currency): String {
        if (currency == Currency.TRY) return "TRY · Ana para birimi"
        val rate = market.tryPrice(currency.code)?.takeIf { it > 0 } ?: return currency.code
        return "${currency.code} · 1 ${currency.code} = ${TrFormat.money(rate)}"
    }

    fun select(currency: Currency) = viewModelScope.launch {
        val previous = selected.value
        // Zaten seçili olana tekrar basmak "değişim" değil; currency_changed şişmesin.
        if (previous == currency) return@launch
        prefs.setSelectedCurrency(currency)
        analytics.logCurrencyChanged(previous.code, currency.code)
    }
}

/**
 * Portföy tutarının gösterileceği para birimi. Bakiye kartındaki ve ayarlardaki
 * açılır menülerin yerine tek tam ekran: on beş seçenek menüye sığmıyordu,
 * ekran ayrıca güncel kuru gösteriyor. Yalnızca seçili satırda işaret var.
 * iOS `CurrencySelectionView` portu.
 */
@Composable
fun CurrencySelectionScreen(
    onClose: () -> Unit,
    viewModel: CurrencySelectionViewModel = hiltViewModel()
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val currencies = Currency.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        ScreenNavBar(title = "Para Birimi", onClose = onClose)
        Text(
            "Portföyün bu para biriminde gösterilir",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            itemsIndexed(currencies) { index, currency ->
                val shape = when {
                    currencies.size == 1 -> RoundedCornerShape(16.dp)
                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    index == currencies.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Column(
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.select(currency)
                                onClose()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(currency.flag, fontSize = 24.sp, modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currency.displayName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                viewModel.subtitle(currency),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (currency == selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Seçili",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < currencies.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                    }
                }
            }
        }
    }
}
