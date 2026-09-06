package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import kotlin.math.abs

/**
 * Portföyün gradyanlı bakiye kartı: toplam değer, kâr/zarar rozeti, para birimi
 * seçici ve tutarları gizleme (göz) düğmesi. iOS `BalanceCardView` portu.
 */
@Composable
fun BalanceCard(
    portfolioColor: PortfolioColor,
    metrics: PortfolioMetrics,
    currency: Currency,
    onCurrencyChange: (Currency) -> Unit,
    /** null ise göz düğmesi gizlenir (onboarding örnekleri). */
    valuesMasked: Boolean?,
    onToggleMask: () -> Unit,
    convert: (Double) -> Double,
    modifier: Modifier = Modifier
) {
    val masked = valuesMasked == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = portfolioColor.gradient,
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            // Sağ üstteki yumuşak ışık halkası — iOS'taki dekoratif daire.
            // Layout child'ı olarak eklenirse 180dp'lik boyu kartın yüksekliğini
            // dayatıyor (offset yerleşimi kaydırır, ölçüyü değiştirmez) ve kartın
            // altında kocaman bir boşluk kalıyordu. Bu yüzden çiziliyor.
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = 90.dp.toPx(),
                    center = Offset(size.width - 30.dp.toPx(), 20.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Toplam Bakiye",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                if (valuesMasked != null) {
                    MaskToggle(masked = masked, onToggle = onToggleMask)
                }
                CurrencyMenu(selected = currency, onSelect = onCurrencyChange)
            }

            Text(
                text = if (masked) TrFormat.MASK
                else TrFormat.money(convert(metrics.totalValue), currency),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (metrics.hasProfitLoss) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (metrics.isPositive) Icons.Filled.ArrowUpward
                            else Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "%${TrFormat.decimal(abs(metrics.profitLossPercent))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("·", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (masked) TrFormat.MASK else buildString {
                                append(if (metrics.isPositive) "+" else "-")
                                append(TrFormat.money(abs(convert(metrics.profitLoss)), currency))
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Kâr / Zarar",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (metrics.hasMissingPrices) {
                Text(
                    text = "Bazı varlıkların fiyatı henüz alınamadı.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun MaskToggle(masked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (masked) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (masked) "Tutarları göster" else "Tutarları gizle",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CurrencyMenu(selected: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selected.code,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.displayName) },
                    trailingIcon = {
                        if (currency == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    }
                )
            }
        }
    }
}
