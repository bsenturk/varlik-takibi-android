package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    /** Para birimi seçim ekranını açar; null ise çip salt gösterim (onboarding). */
    onCurrencyClick: (() -> Unit)?,
    /** null ise göz düğmesi gizlenir (onboarding örnekleri). */
    valuesMasked: Boolean?,
    onToggleMask: () -> Unit,
    convert: (Double) -> Double,
    modifier: Modifier = Modifier,
    /** Değer hedefi (TL). 0 → çubuk yerine "Hedef belirle" kısayolu. */
    targetValue: Double = 0.0,
    /**
     * Hedef düzenleyiciyi açar. null ise hedef salt okunur: "Genel"de hedef
     * türetilmiş bir toplam, onboarding örneklerinde de hiç gösterilmiyor.
     */
    onSetTarget: (() -> Unit)? = null
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
                CurrencyChip(selected = currency, onClick = onCurrencyClick)
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

            // Düzenlenebiliyorsa her zaman (boşken "Hedef belirle" kısayolu),
            // salt okunur hâlde yalnızca gerçekten bir hedef varsa.
            if (onSetTarget != null || targetValue > 0) {
                TargetSection(
                    progress = if (targetValue > 0) maxOf(0.0, metrics.totalValue / targetValue) else 0.0,
                    targetText = if (masked) TrFormat.MASK
                    else TrFormat.money(convert(targetValue), currency),
                    hasTarget = targetValue > 0,
                    onSetTarget = onSetTarget
                )
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
private fun TargetSection(
    progress: Double,
    targetText: String,
    hasTarget: Boolean,
    onSetTarget: (() -> Unit)?
) {
    if (!hasTarget) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onSetTarget?.invoke() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.TrackChanges, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text("Hedef belirle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        return
    }

    val reached = progress >= 1
    val tint = Color.White.copy(alpha = 0.9f)
    Column(
        modifier = if (onSetTarget != null) Modifier.clickable(onClick = onSetTarget) else Modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (reached) Icons.Filled.Verified else Icons.Filled.TrackChanges,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            // "Genel"de etiket her hâlükârda "Toplam hedef" kalıyor: oranın
            // türetilmiş bir toplamdan geldiği kaybolmasın.
            Text(
                text = when {
                    onSetTarget == null -> "Toplam hedef"
                    reached -> "Hedefe ulaşıldı"
                    else -> "Hedef"
                },
                fontSize = 13.sp,
                color = tint,
                modifier = Modifier.weight(1f)
            )
            Text("%${targetPercentText(progress)}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = tint)
            Text("·", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Text(
                text = targetText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0.0, 1.0).toFloat())
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White)
            )
        }
    }
}

/** %0,4 gibi küçük oranlar 0 görünmesin diye iki basamağa kadar iniyor. */
internal fun targetPercentText(ratio: Double): String {
    val pct = ratio * 100
    val decimals = when {
        pct >= 10 -> 0
        pct >= 1 -> 1
        else -> 2
    }
    return TrFormat.decimal(pct, decimals)
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
private fun CurrencyChip(selected: Currency, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.18f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
}
