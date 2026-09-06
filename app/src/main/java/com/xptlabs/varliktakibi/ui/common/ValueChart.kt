package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ChartPoint(val day: Long, val value: Double)

/**
 * Analiz sekmesindeki portföy değeri grafiği: çizgi + degrade dolgu + yatay
 * kılavuz çizgileri ve alt tarafta tarih etiketleri.
 *
 * Grafik kütüphanesi yerine düz Canvas: tek bir çizgi grafiği için üç ek
 * bağımlılık taşımaya değmiyor ve sparkline zaten aynı yolu çiziyor.
 * ponytail: etkileşim (dokununca değer okuma) yok; istenirse eklenir.
 */
@Composable
fun ValueChart(
    points: List<ChartPoint>,
    lineColor: Color,
    height: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        // Boş durumda grafik yüksekliğini ayırmıyoruz — tek satırlık mesaj için
        // 180dp boşluk bırakmak ekranın yarısını yiyordu.
        Text(
            text = "Grafik için henüz yeterli veri yok.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    val gridColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val values = points.map { it.value }
            val min = values.min()
            val max = values.max()
            val range = (max - min).takeIf { it > 0 } ?: 1.0

            // Yatay kılavuz çizgileri (4 aralık).
            val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
            repeat(5) { i ->
                val y = size.height * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = dash
                )
            }

            val stepX = size.width / (points.size - 1)
            val offsets = points.mapIndexed { index, point ->
                val normalized = (point.value - min) / range
                // Üstte ve altta %8 pay: tepe ve dip kırpılmasın.
                val y = size.height - (normalized.toFloat() * size.height * 0.84f +
                    size.height * 0.08f)
                Offset(index * stepX, y)
            }

            drawPath(
                path = Path().apply {
                    moveTo(offsets.first().x, size.height)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, size.height)
                    close()
                },
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            drawPath(
                path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                },
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(points.first(), points[points.size / 2], points.last()).forEach { point ->
                Text(
                    text = formatter.format(
                        com.xptlabs.varliktakibi.core.ext.Days.toLocalDate(point.day)
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr-TR"))
