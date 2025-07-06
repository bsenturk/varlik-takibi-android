package com.xptlabs.varliktakibi.presentation.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground
import com.xptlabs.varliktakibi.presentation.assets.components.AssetFormDialog
import com.xptlabs.varliktakibi.presentation.assets.components.AssetCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    navController: NavController,
    viewModel: AssetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddAssetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAssets()
    }

    // Error handling
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            // TODO: Show snackbar or error dialog
            // For now, just clear the error after showing
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with total value - only show if user has assets
        if (uiState.assets.isNotEmpty()) {
            TotalValueHeader(
                totalValue = uiState.totalPortfolioValue,
                profitLoss = uiState.profitLoss,
                profitLossPercentage = uiState.profitLossPercentage,
                onAnalyticsClick = {
                    // Navigate to analytics
                }
            )
        }

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                // İlk yükleme sırasında loading göster
                uiState.isLoading && !uiState.hasDataLoaded -> {
                    LoadingStateView()
                }

                // Veri yüklenmiş ama varlık yok - Empty State
                uiState.hasDataLoaded && uiState.assets.isEmpty() -> {
                    EmptyStateView(
                        onAddAssetClick = { showAddAssetDialog = true }
                    )
                }

                // Varlıklar var - Liste göster
                else -> {
                    AssetListView(
                        assets = uiState.assets,
                        isRefreshing = uiState.isRefreshing,
                        onEditAsset = { asset ->
                            // Navigate to edit - TODO: implement
                        },
                        onDeleteAsset = { asset ->
                            viewModel.deleteAsset(asset)
                        },
                        onRefresh = {
                            viewModel.refreshData()
                        }
                    )
                }
            }

            // Floating Action Button - only show if data is loaded
            this@Column.AnimatedVisibility(
                visible = uiState.hasDataLoaded && !uiState.assets.isEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                FloatingActionButton(
                    onClick = { showAddAssetDialog = true },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Varlık Ekle",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Add Asset Dialog
    if (showAddAssetDialog) {
        AssetFormDialog(
            onDismiss = { showAddAssetDialog = false },
            onSave = { asset ->
                viewModel.addAsset(asset)
                showAddAssetDialog = false
            },
            marketDataManager = viewModel.marketDataManager
        )
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
                text = "Güncel kurlar yükleniyor...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Altın ve döviz kurları getiriliyor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TotalValueHeader(
    totalValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double,
    onAnalyticsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Toplam Varlık",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Text(
                        text = formatCurrency(totalValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Profit/Loss info
                    if (profitLoss != 0.0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (profitLoss >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (profitLoss >= 0) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f)
                            )

                            Text(
                                text = formatProfitLossPercentage(profitLossPercentage, profitLoss),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (profitLoss >= 0) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f)
                            )

                            Text(
                                text = "(${if (profitLoss >= 0) "+" else ""}${formatCurrency(profitLoss)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onAnalyticsClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analitik",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    onAddAssetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconWithBackground(
            icon = Icons.Default.AccountBalanceWallet,
            contentDescription = "Empty Wallet",
            size = 100.dp,
            iconSize = 50.dp,
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Henüz varlık eklenmemiş",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "İlk varlığınızı ekleyerek portföy takibine başlayın. Altın, döviz ve diğer varlıklarınızı kolayca yönetebilirsiniz.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )

        Spacer(modifier = Modifier.height(32.dp))

        GradientButton(
            text = "İlk Varlığını Ekle",
            onClick = onAddAssetClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AssetListView(
    assets: List<Asset>,
    isRefreshing: Boolean,
    onEditAsset: (Asset) -> Unit,
    onDeleteAsset: (Asset) -> Unit,
    onRefresh: () -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Varlıklarım",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${assets.size} varlık",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                items = assets,
                key = { it.id }
            ) { asset ->
                AssetCard(
                    asset = asset,
                    onEdit = { onEditAsset(asset) },
                    onDelete = { onDeleteAsset(asset) }
                )
            }

            // Extra space for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Helper functions
private fun formatCurrency(amount: Double): String {
    return "₺${String.format("%,.2f", amount)}"
}

private fun formatProfitLossPercentage(percentage: Double, profitLoss: Double): String {
    val absPercentage = kotlin.math.abs(percentage)

    return if (absPercentage < 0.01 && profitLoss != 0.0) {
        "${if (profitLoss >= 0) "+" else ""}<0,01%"
    } else {
        val sign = if (profitLoss >= 0) "+" else ""
        "$sign${String.format("%.2f", percentage)}%"
    }
}