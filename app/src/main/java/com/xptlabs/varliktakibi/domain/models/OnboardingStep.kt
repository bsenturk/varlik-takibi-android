package com.xptlabs.varliktakibi.domain.models

import androidx.compose.ui.graphics.Color

data class OnboardingStep(
    val icon: String,
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)