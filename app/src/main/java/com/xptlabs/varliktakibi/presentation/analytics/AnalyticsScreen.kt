package com.xptlabs.varliktakibi.presentation.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.xptlabs.varliktakibi.BuildConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with back button
        TopAppBar(
            title = {
                Text(
                    text = "Varlık Analizi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Debug section only in debug builds
            /*if (BuildConfig.DEBUG) {
                item {
                    DebugAnalyticsSection(viewModel = viewModel)
                }
            }*/

            // Total Portfolio Value Card
            item {
                TotalPortfolioCard(
                    totalValue = uiState.totalValue,
                    profitLoss = uiState.profitLoss,
                    profitLossPercentage = uiState.profitLossPercentage
                )
            }

            // Comparison Chart (only if there's meaningful data)
            if (uiState.hasProfitLossData) {
                item {
                    ComparisonChart(
                        totalInvestment = uiState.totalInvestment,
                        currentValue = uiState.totalValue,
                        profitLoss = uiState.profitLoss,
                        profitLossPercentage = uiState.profitLossPercentage
                    )
                }
            }

            // Asset Distribution
            item {
                AssetDistributionCard(
                    distributions = uiState.assetDistributions
                )
            }

            // Extra space at bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TotalPortfolioCard(
    totalValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Toplam Portföy Değeri",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatCurrency(totalValue),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Profit/Loss indicator
            if (abs(profitLoss) > 0.01) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (profitLoss >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    Text(
                        text = formatProfitLossPercentage(profitLossPercentage, profitLoss),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    Text(
                        text = "(${if (profitLoss >= 0) "+" else ""}${formatCurrency(profitLoss)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonChart(
    totalInvestment: Double,
    currentValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Maliyet vs Güncel Değer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Investment Cost Bar
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedBar(
                        height = 80.dp,
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.8f),
                            Color(0xFF2196F3).copy(alpha = 0.3f)
                        )
                    )

                    Text(
                        text = "Maliyet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatCurrency(totalInvestment),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2196F3),
                        textAlign = TextAlign.Center
                    )
                }

                // Current Value Bar
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentValueBarHeight = calculateCurrentValueBarHeight(totalInvestment, currentValue)
                    val barColor = if (profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

                    AnimatedBar(
                        height = currentValueBarHeight,
                        colors = listOf(
                            barColor.copy(alpha = 0.8f),
                            barColor.copy(alpha = 0.3f)
                        )
                    )

                    Text(
                        text = "Güncel Değer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatCurrency(currentValue),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = barColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Investment Summary
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Toplam Yatırım",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatCurrency(totalInvestment),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2196F3)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Güncel Değer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatCurrency(currentValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Kar/Zarar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${if (profitLoss >= 0) "+" else ""}${formatCurrency(profitLoss)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Getiri Oranı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatProfitLossPercentage(profitLossPercentage, profitLoss),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBar(
    height: Dp,
    colors: List<Color>
) {
    val animatedHeight by animateFloatAsState(
        targetValue = height.value,
        animationSpec = tween(durationMillis = 1000),
        label = "bar_height"
    )

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(animatedHeight.dp)
            .background(
                Brush.verticalGradient(colors),
                RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun AssetDistributionCard(
    distributions: List<AssetDistribution>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Varlık Dağılımı",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (distributions.isEmpty()) {
                Text(
                    text = "Henüz varlık bulunmuyor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    distributions.forEach { distribution ->
                        DistributionItem(distribution = distribution)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionItem(
    distribution: AssetDistribution
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(distribution.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(distribution.color)
            )
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = distribution.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formatCurrency(distribution.value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Percentage
        Text(
            text = formatDistributionPercentage(distribution.percentage),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DebugAnalyticsSection(viewModel: AnalyticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🐛 DEBUG - Analytics Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Test verileri ekleyerek analytics'i test edebilirsiniz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.generateRandomTestData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Test Verisi Ekle", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { viewModel.clearTestData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Temizle", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                text = "• Test Verisi Ekle: Kar/zarar senaryoları\n• Temizle: Tüm varlıkları sil",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper functions
private fun calculateCurrentValueBarHeight(investment: Double, currentValue: Double): Dp {
    if (investment <= 0) return 80.dp

    val ratio = currentValue / investment
    val height = ratio * 80 // Base height of investment bar

    return max(40f, min(120f, height.toFloat())).dp
}

private fun formatCurrency(amount: Double): String {
    return "₺${String.format("%,.2f", amount)}"
}

private fun formatProfitLossPercentage(percentage: Double, profitLoss: Double): String {
    val absPercentage = abs(percentage)

    return if (absPercentage < 0.01 && profitLoss != 0.0) {
        "${if (profitLoss >= 0) "+" else ""}<0,01%"
    } else {
        val sign = if (profitLoss >= 0) "+" else ""
        "$sign${String.format("%.2f", percentage)}%"
    }
}

private fun formatDistributionPercentage(percentage: Double): String {
    return when {
        percentage < 0.01 && percentage > 0 -> "<0,01%"
        percentage >= 99.99 && percentage < 100 -> "99,99%"
        else -> "${String.format("%.2f", percentage)}%"
    }
}