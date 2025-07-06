package com.xptlabs.varliktakibi.presentation.assets.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCard(
    asset: Asset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Asset Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getAssetColor(asset.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAssetIcon(asset.type),
                    contentDescription = asset.name,
                    modifier = Modifier.size(24.dp),
                    tint = getAssetColor(asset.type)
                )
            }

            // Asset Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formatAmount(asset.amount, asset.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Value and Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCurrency(asset.totalValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Varlık Sil")
            },
            text = {
                Text("${asset.name} varlığınızı silmek istediğinizden emin misiniz?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        "Sil",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("İptal")
                }
            }
        )
    }
}

fun getAssetIcon(assetType: AssetType): ImageVector {
    return when (assetType) {
        AssetType.GOLD,
        AssetType.GOLD_QUARTER,
        AssetType.GOLD_HALF,
        AssetType.GOLD_FULL,
        AssetType.GOLD_REPUBLIC,
        AssetType.GOLD_ATA,
        AssetType.GOLD_RESAT,
        AssetType.GOLD_HAMIT -> Icons.Default.MonetizationOn

        AssetType.USD -> Icons.Default.AttachMoney
        AssetType.EUR -> Icons.Default.Euro
        AssetType.GBP -> Icons.Default.CurrencyPound
        AssetType.TRY -> Icons.Default.CurrencyLira
    }
}

fun getAssetColor(assetType: AssetType): Color {
    return when (assetType) {
        AssetType.GOLD,
        AssetType.GOLD_QUARTER,
        AssetType.GOLD_HALF,
        AssetType.GOLD_FULL,
        AssetType.GOLD_REPUBLIC,
        AssetType.GOLD_ATA,
        AssetType.GOLD_RESAT,
        AssetType.GOLD_HAMIT -> Color(0xFFFFD700) // Gold

        AssetType.USD -> Color(0xFF4CAF50) // Green
        AssetType.EUR -> Color(0xFF2196F3) // Blue
        AssetType.GBP -> Color(0xFF9C27B0) // Purple
        AssetType.TRY -> Color(0xFFF44336) // Red
    }
}

fun formatAmount(amount: Double, unit: String): String {
    return if (amount % 1.0 == 0.0) {
        "${amount.toInt()} $unit"
    } else {
        "${String.format("%.3f", amount).trimEnd('0').trimEnd('.')} $unit"
    }
}

fun formatCurrency(amount: Double): String {
    return "₺${String.format("%,.2f", amount)}"
}