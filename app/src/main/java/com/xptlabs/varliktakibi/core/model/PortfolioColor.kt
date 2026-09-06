package com.xptlabs.varliktakibi.core.model

import androidx.compose.ui.graphics.Color
import com.xptlabs.varliktakibi.core.ext.toColor

/** Portföy oluştururken seçilen sabit palet (iOS PortfolioColor.swift). */
enum class PortfolioColor(val hex: String, private val gradientHex: Pair<String, String>) {
    BLUE("#007AFF", "#0A84FF" to "#5E5CE6"),
    PURPLE("#AF52DE", "#AF52DE" to "#BF5AF2"),
    PINK("#FF2D55", "#FF2D55" to "#FF375F"),
    GREEN("#34C759", "#34C759" to "#30D158"),
    ORANGE("#FF9500", "#FF9F0A" to "#FF9500"),
    RED("#FF3B30", "#FF3B30" to "#FF453A");

    val color: Color get() = hex.toColor()

    /** Seçili chip ve bakiye kartındaki degrade. */
    val gradient: List<Color>
        get() = listOf(gradientHex.first.toColor(), gradientHex.second.toColor())

    companion object {
        fun fromHex(hex: String?): PortfolioColor =
            entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: BLUE
    }
}
