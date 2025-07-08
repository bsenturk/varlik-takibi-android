package com.xptlabs.varliktakibi.presentation.rates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground
import com.xptlabs.varliktakibi.presentation.rates.components.RateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(
    viewModel: RatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // Analytics
        viewModel.analyticsManager.logRatesViewed(
            goldRateCount = uiState.goldRates.size,
            currencyRateCount = uiState.currencyRates.size
        )
    }

    // Error handling
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }

        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            icon = {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Hata")
            },
            text = {
                Text(error)
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearError() }
                ) {
                    Text("Tamam")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Güncel Kurlar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Content
        when {
            // Loading ve veri yok
            uiState.isRefreshing && uiState.currencyRates.isEmpty() && uiState.goldRates.isEmpty() -> {
                LoadingStateView()
            }

            // Veri var - Liste göster
            else -> {
                RateListView(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshRates() }
                )
            }
        }
    }
}

@Composable
private fun LoadingStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Kurlar yükleniyor...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RateListView(
    uiState: RatesUiState,
    onRefresh: () -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Currency Rates Section
            if (uiState.currencyRates.isNotEmpty()) {
                item {
                    Text(
                        text = "Döviz Kurları",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.currencyRates,
                    key = { it.id }
                ) { rate ->
                    RateCard(rate = rate)
                }

                // Spacer between sections
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Gold Rates Section
            if (uiState.goldRates.isNotEmpty()) {
                item {
                    Text(
                        text = "Altın Fiyatları",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.goldRates,
                    key = { it.id }
                ) { rate ->
                    RateCard(rate = rate)
                }
            }

            // Empty state
            if (uiState.currencyRates.isEmpty() && uiState.goldRates.isEmpty() && !uiState.isRefreshing) {
                item {
                    EmptyRatesView(onRefresh = onRefresh)
                }
            }
        }
    }
}

@Composable
private fun EmptyRatesView(
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconWithBackground(
            icon = Icons.Default.TrendingUp,
            contentDescription = "No Rates",
            size = 80.dp,
            iconSize = 40.dp,
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        Text(
            text = "Henüz kur verisi yok",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Güncel altın ve döviz kurlarını görmek için sayfayı yenileyin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Kurları Yenile")
        }
    }
}