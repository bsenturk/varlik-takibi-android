package com.xptlabs.varliktakibi.presentation.assetdetail

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.xptlabs.varliktakibi.data.local.entities.TransactionType
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetId: String,
    navController: NavController,
    viewModel: AssetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(assetId) {
        viewModel.loadAssetDetail(assetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.asset?.name ?: "Varlık Detayı") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.asset != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Asset Summary Card
                item {
                    AssetSummaryCard(uiState = uiState)
                }

                // Price Chart Section
                item {
                    PriceChartCard(uiState = uiState, viewModel = viewModel)
                }

                // Value Comparison Card
                item {
                    ValueComparisonCard(uiState = uiState)
                }

                // Transaction History Section
                item {
                    TransactionHistoryCard(uiState = uiState)
                }

                // Price History Section
                item {
                    PriceHistoryCard(uiState = uiState)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Varlık bulunamadı",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AssetSummaryCard(uiState: AssetDetailUiState) {
    val asset = uiState.asset ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = asset.type.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Miktar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${String.format("%.2f", asset.amount)} ${asset.unit}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Güncel Değer",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrency(asset.totalValue),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceChartCard(uiState: AssetDetailUiState, viewModel: AssetDetailViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fiyat Grafiği",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Period Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartPeriod.values().forEach { period ->
                    FilterChip(
                        selected = uiState.selectedChartPeriod == period,
                        onClick = { viewModel.setChartPeriod(period) },
                        label = { Text(period.label) }
                    )
                }
            }

            // Vico Chart
            if (uiState.chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu dönem için veri yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val modelProducer = remember { CartesianChartModelProducer.build() }

                LaunchedEffect(uiState.chartData) {
                    modelProducer.tryRunTransaction {
                        lineSeries {
                            series(uiState.chartData.map { it.value })
                        }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
private fun ValueComparisonCard(uiState: AssetDetailUiState) {
    val asset = uiState.asset ?: return
    val profitLoss = asset.totalValue - asset.totalInvestment
    val profitLossPercentage = if (asset.totalInvestment > 0) {
        (profitLoss / asset.totalInvestment) * 100
    } else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kar/Zarar Analizi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "İlk Değer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(asset.totalInvestment),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Güncel Değer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(asset.totalValue),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (profitLoss >= 0) "Kar" else "Zarar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (profitLoss >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (profitLoss >= 0) Color.Green else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(kotlin.math.abs(profitLoss)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (profitLoss >= 0) Color.Green else Color.Red
                        )
                        Text(
                            text = "${if (profitLoss >= 0) "+" else ""}${String.format("%.2f", profitLossPercentage)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (profitLoss >= 0) Color.Green else Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryCard(uiState: AssetDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İşlem Geçmişi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(Son 10 işlem)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.transactionHistory.isEmpty()) {
                Text(
                    text = "Henüz işlem geçmişi yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.transactionHistory.forEach { transaction ->
                        TransactionHistoryItem(transaction)
                        if (transaction != uiState.transactionHistory.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryItem(transaction: com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Transaction Type Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        getTransactionColor(transaction.transactionType).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTransactionIcon(transaction.transactionType),
                    contentDescription = null,
                    tint = getTransactionColor(transaction.transactionType),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = getTransactionLabel(transaction.transactionType),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (transaction.transactionType == TransactionType.ADD) "+" else if (transaction.transactionType == TransactionType.REMOVE) "-" else ""}${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (transaction.transactionType) {
                    TransactionType.ADD -> Color.Green
                    TransactionType.REMOVE -> Color.Red
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "Toplam: ${String.format("%.2f", transaction.totalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getTransactionIcon(type: TransactionType) = when (type) {
    TransactionType.INITIAL -> Icons.Default.AddCircle
    TransactionType.ADD -> Icons.Default.Add
    TransactionType.REMOVE -> Icons.Default.Remove
    TransactionType.EDIT -> Icons.Default.Edit
}

private fun getTransactionColor(type: TransactionType) = when (type) {
    TransactionType.INITIAL -> Color(0xFF4CAF50)
    TransactionType.ADD -> Color(0xFF2196F3)
    TransactionType.REMOVE -> Color(0xFFF44336)
    TransactionType.EDIT -> Color(0xFFFF9800)
}

private fun getTransactionLabel(type: TransactionType) = when (type) {
    TransactionType.INITIAL -> "İlk Ekleme"
    TransactionType.ADD -> "Ekleme"
    TransactionType.REMOVE -> "Çıkarma"
    TransactionType.EDIT -> "Düzenleme"
}

@Composable
private fun PriceHistoryCard(uiState: AssetDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fiyat Geçmişi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(Son 30 kayıt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.priceHistory.isEmpty()) {
                Text(
                    text = "Henüz fiyat geçmişi yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.priceHistory.take(10).forEach { priceRecord ->
                        PriceHistoryItem(priceRecord)
                        if (priceRecord != uiState.priceHistory.take(10).last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (uiState.priceHistory.size > 10) {
                        Text(
                            text = "+${uiState.priceHistory.size - 10} kayıt daha",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryItem(priceRecord: com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = formatDate(priceRecord.date),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${String.format("%.2f", priceRecord.amount)} adet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(priceRecord.totalValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Birim: ${formatCurrency(priceRecord.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
    return formatter.format(date)
}

private fun formatCurrency(amount: Double): String {
    return "₺${String.format("%,.2f", amount)}"
}

enum class ChartPeriod(val label: String) {
    DAILY("Günlük"),
    WEEKLY("Haftalık"),
    MONTHLY("Aylık")
}
