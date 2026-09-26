package com.xptlabs.varliktakibi.ui.addasset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.data.remote.ChartRange
import com.xptlabs.varliktakibi.data.remote.PriceSeries
import com.xptlabs.varliktakibi.ui.common.Sparkline
import com.xptlabs.varliktakibi.ui.theme.AppColors
import kotlin.math.abs

/**
 * Varlık ekleme akışının son adımındaki enstrüman detayı: güncel fiyat, günlük
 * değişim ve seçilebilir aralıklı fiyat grafiği. Yalnızca dinamik kategorilerde
 * (kripto/hisse/ETF/fon) — altın ve dövizin geçmiş kaynağı yok.
 * iOS `InstrumentChartCard` portu.
 */
@Composable
fun InstrumentChartCard(
    symbol: String,
    category: AssetCategory,
    tint: Color,
    /** TL cinsinden güncel birim fiyat (uygulamanın her yerindeki fiyatla aynı). */
    currentPrice: Double,
    dayChangePercent: Double?,
    loadSeries: suspend (String, ChartRange) -> PriceSeries
) {
    // TEFAS tek istekte ~30 günden fazlasını vermiyor, art arda isteklerde 429
    // dönüyor: fonlarda uzun aralıklar hiç gösterilmiyor.
    val ranges = if (category == AssetCategory.FUND) listOf(ChartRange.WEEK, ChartRange.MONTH)
    else ChartRange.entries
    var range by remember(symbol) { mutableStateOf(ChartRange.MONTH) }
    // Aralık başına önbellek: 1A ↔ 1Y arasında gidip gelirken tekrar çekilmesin.
    val cache = remember(symbol) { mutableStateMapOf<ChartRange, PriceSeries>() }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(symbol, range) {
        if (cache.containsKey(range)) return@LaunchedEffect
        loading = true
        failed = false
        runCatching { loadSeries(symbol, range) }
            .onSuccess { cache[range] = it }
            .onFailure { failed = true }
        loading = false
    }

    val series = cache[range]
    val values = series?.points?.map { it.c }.orEmpty()
    val trend = when {
        values.size < 2 -> tint
        values.last() >= values.first() -> AppColors.positive
        else -> AppColors.negative
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Güncel Fiyat", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(TrFormat.money(currentPrice), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
            dayChangePercent?.let { ChangeBadge(it) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                values.size > 1 -> Sparkline(values = values, lineColor = trend, modifier = Modifier.fillMaxWidth().height(96.dp))
                loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else -> Text(
                    if (failed) "Grafik verisi alınamadı." else "Bu varlık için grafik verisi yok.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (series?.currency == "USD" && values.size > 1) {
            Text("Grafik USD fiyatı üzerinden", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ranges.forEach { option ->
                val selected = option == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) tint else AppColors.subtleFill)
                        .clickable { range = option },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeBadge(change: Double) {
    val color = if (change >= 0) AppColors.positive else AppColors.negative
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (change >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Text("%${TrFormat.decimal(abs(change))}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text("Bugün", fontSize = 12.sp, color = color.copy(alpha = 0.7f))
    }
}
