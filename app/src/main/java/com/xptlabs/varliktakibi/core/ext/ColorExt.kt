package com.xptlabs.varliktakibi.core.ext

import androidx.compose.ui.graphics.Color

/** "#RRGGBB" ya da "RRGGBB" → Color. Geçersizse iOS ile aynı fallback: sistem mavisi. */
fun String.toColor(): Color {
    val cleaned = trim().removePrefix("#")
    val rgb = cleaned.toLongOrNull(16)
    if (rgb == null || cleaned.length != 6) return Color(0xFF007AFF)
    return Color(0xFF000000L or rgb)
}
