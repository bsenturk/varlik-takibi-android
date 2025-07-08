package com.xptlabs.varliktakibi.domain.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class RateDisplayModel(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val buyRate: String,
    val sellRate: String,
    val change: String,
    val isChangeRatePositive: Boolean
)