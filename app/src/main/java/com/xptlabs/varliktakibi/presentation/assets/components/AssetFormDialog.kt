package com.xptlabs.varliktakibi.presentation.assets.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import java.util.*
import  com.xptlabs.varliktakibi.managers.MarketDataManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetFormDialog(
    asset: Asset? = null, // null means adding new asset
    onDismiss: () -> Unit,
    onSave: (Asset) -> Unit,
    marketDataManager: MarketDataManager
) {
    val isEditMode = asset != null

    var selectedAssetType by remember { mutableStateOf(asset?.type ?: AssetType.GOLD) }
    var amount by remember { mutableStateOf(asset?.amount?.let { formatAmountForEditing(it) } ?: "") }
    var showAssetTypeDropdown by remember { mutableStateOf(false) }

    // Mock current rates
    val currentRate = getCurrentRate(selectedAssetType, marketDataManager)
    val totalValue = calculateTotalValue(amount, currentRate)

    val isValidInput = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Varlık Düzenle" else "Varlık Ekle",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isEditMode) "Varlık bilgilerinizi güncelleyin" else "Yeni bir varlık ekleyerek portföyünüzü genişletin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Asset Type Selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Varlık Türü",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = showAssetTypeDropdown,
                        onExpandedChange = { showAssetTypeDropdown = !showAssetTypeDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedAssetType.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Varlık türünü seçin") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAssetTypeDropdown)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = getFormAssetIcon(selectedAssetType),
                                    contentDescription = null,
                                    tint = getFormAssetColor(selectedAssetType)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = showAssetTypeDropdown,
                            onDismissRequest = { showAssetTypeDropdown = false }
                        ) {
                            AssetType.values().forEach { assetType ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = getFormAssetIcon(assetType),
                                                contentDescription = null,
                                                tint = getFormAssetColor(assetType),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = assetType.displayName,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Birim: ${assetType.unit}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedAssetType = assetType
                                        showAssetTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Input
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Miktar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Allow only numbers and decimal point
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                                .replace(',', '.')

                            // Ensure only one decimal point
                            val dotCount = filtered.count { it == '.' }
                            if (dotCount <= 1) {
                                // Limit decimal places to 3
                                val parts = filtered.split('.')
                                amount = if (parts.size == 2 && parts[1].length > 3) {
                                    "${parts[0]}.${parts[1].take(3)}"
                                } else {
                                    filtered
                                }
                            }
                        },
                        label = { Text("Miktar girin") },
                        suffix = { Text(selectedAssetType.unit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            if (amount.isNotBlank() && amount.toDoubleOrNull() == null) {
                                Text(
                                    "Geçerli bir miktar girin",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = amount.isNotBlank() && amount.toDoubleOrNull() == null
                    )
                }

                // Current Rate Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
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
                            Column {
                                Text(
                                    text = "Güncel Kur",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(currentRate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (totalValue > 0) {
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Toplam Değer",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatCurrency(totalValue),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Kurlar anlık olarak değişebilir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton(
                        text = if (isEditMode) "Güncelle" else "Varlık Ekle",
                        onClick = {
                            val amountValue = amount.toDoubleOrNull() ?: 0.0
                            val newAsset = Asset(
                                id = asset?.id ?: UUID.randomUUID().toString(),
                                type = selectedAssetType,
                                name = selectedAssetType.displayName,
                                amount = amountValue,
                                unit = selectedAssetType.unit,
                                purchasePrice = currentRate,
                                currentPrice = currentRate,
                                dateAdded = asset?.dateAdded ?: Date(),
                                lastUpdated = Date()
                            )
                            onSave(newAsset)
                        },
                        enabled = isValidInput
                    )

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal")
                    }
                }
            }
        }
    }
}

private fun formatAmountForEditing(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        // Tam sayı ise decimal olmadan göster
        amount.toInt().toString()
    } else {
        // Decimal varsa, gereksiz sıfırları kaldır
        String.format("%.3f", amount).trimEnd('0').trimEnd('.')
    }
}

private fun getCurrentRate(assetType: AssetType, marketDataManager: MarketDataManager): Double {
    return marketDataManager.getCurrentPrice(assetType)
}

// Toplam değer hesapla
private fun calculateTotalValue(amountStr: String, currentRate: Double): Double {
    val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    return amount * currentRate
}

// Asset icon'ı al - form dialog için
private fun getFormAssetIcon(assetType: AssetType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (assetType) {
        AssetType.GOLD,
        AssetType.GOLD_QUARTER,
        AssetType.GOLD_HALF,
        AssetType.GOLD_FULL,
        AssetType.GOLD_REPUBLIC,
        AssetType.GOLD_ATA,
        AssetType.GOLD_RESAT,
        AssetType.GOLD_HAMIT -> Icons.Default.Star // Altın için yıldız

        AssetType.USD,
        AssetType.EUR,
        AssetType.GBP,
        AssetType.TRY -> Icons.Default.AttachMoney // Döviz için para ikonu
    }
}

// Asset rengi al - form dialog için
private fun getFormAssetColor(assetType: AssetType): androidx.compose.ui.graphics.Color {
    return when (assetType) {
        AssetType.GOLD,
        AssetType.GOLD_QUARTER,
        AssetType.GOLD_HALF,
        AssetType.GOLD_FULL,
        AssetType.GOLD_REPUBLIC,
        AssetType.GOLD_ATA,
        AssetType.GOLD_RESAT,
        AssetType.GOLD_HAMIT -> Color(0xFFFFD700) // Altın rengi

        AssetType.USD -> Color(0xFF4CAF50) // Yeşil
        AssetType.EUR -> Color(0xFF2196F3) // Mavi
        AssetType.GBP -> Color(0xFF9C27B0) // Mor
        AssetType.TRY -> Color(0xFFF44336) // Kırmızı
    }
}