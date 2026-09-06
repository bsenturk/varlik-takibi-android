package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Varlık satırındaki minik fiyat grafiği. Satır başına bir grafik kütüphanesi
 * kurmak yerine düz Canvas — liste kaydırmada da ucuz.
 *
 * iOS `SparklineView.swift` portu.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()

        if (values.size < 2) {
            // Yeterli veri yoksa düz taban çizgisi — boş bırakmak satırı bozuyor.
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val points = normalize(values, size)

        // Çizginin altındaki yumuşak dolgu.
        drawPath(
            path = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            },
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
            )
        )

        drawPath(
            path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            },
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/** Değerleri kutuya oturtur; tepeler kırpılmasın diye %10 dikey pay bırakır. */
private fun normalize(values: List<Double>, size: Size): List<Offset> {
    val min = values.min()
    val max = values.max()
    val range = max - min
    val stepX = size.width / (values.size - 1)

    return values.mapIndexed { index, value ->
        val normalized = if (range == 0.0) 0.5 else (value - min) / range
        val y = size.height - (normalized.toFloat() * size.height * 0.8f + size.height * 0.1f)
        Offset(index * stepX, y)
    }
}
