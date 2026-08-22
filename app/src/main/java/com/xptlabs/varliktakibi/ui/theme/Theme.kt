package com.xptlabs.varliktakibi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.data.prefs.DarkModePreference

private val LightColors = lightColorScheme(
    primary = SystemBlue,
    onPrimary = Color.White,
    secondary = SystemPurple,
    onSecondary = Color.White,
    tertiary = SystemGreen,
    error = SystemRed,
    onError = Color.White,
    background = GroupedBackgroundLight,
    onBackground = LabelLight,
    surface = CardLight,
    onSurface = LabelLight,
    surfaceVariant = GroupedBackgroundLight,
    onSurfaceVariant = SecondaryLabelLight,
    outline = SeparatorLight,
    outlineVariant = SeparatorLight
)

private val DarkColors = darkColorScheme(
    primary = SystemBlueDark,
    onPrimary = Color.White,
    secondary = SystemPurple,
    onSecondary = Color.White,
    tertiary = SystemGreenDark,
    error = SystemRedDark,
    onError = Color.White,
    background = GroupedBackgroundDark,
    onBackground = LabelDark,
    surface = CardDark,
    onSurface = LabelDark,
    surfaceVariant = ElevatedCardDark,
    onSurfaceVariant = SecondaryLabelDark,
    outline = SeparatorDark,
    outlineVariant = SeparatorDark
)

/**
 * iOS'ta metin boyutları `.system(size:weight:)` ile noktasal veriliyor, semantik
 * stil kullanılmıyor. Aynı görünümü tutturmak için burada da ekranlar boyutu
 * doğrudan veriyor; Typography yalnızca Material bileşenlerinin varsayılanı.
 */
private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
)

/**
 * Kâr yeşili / zarar kırmızısı ve ayırıcı gibi, Material şemasında karşılığı
 * olmayan ama her ekranda gereken renkler.
 */
object AppColors {
    val positive: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark()) SystemGreenDark else SystemGreen

    val negative: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark()) SystemRedDark else SystemRed

    /**
     * Chip, tuş takımı ve segment gibi ikinci seviye zeminler. Açık temada
     * sayfa zemini zaten gri olduğu için bir ton daha koyu olmalı — aynı renk
     * verilirse tuşlar görünmez oluyor.
     */
    val subtleFill: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark()) ElevatedCardDark else FillLight

    @Composable @ReadOnlyComposable
    private fun isDark() = MaterialTheme.colorScheme.background == GroupedBackgroundDark

    /** Kâr/zarara göre renk; sıfıra çok yakınsa nötr. */
    @Composable @ReadOnlyComposable
    fun forChange(value: Double): Color = when {
        value > 0.0001 -> positive
        value < -0.0001 -> negative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun shouldUseDarkTheme(preference: DarkModePreference): Boolean = when (preference) {
    DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    DarkModePreference.LIGHT -> false
    DarkModePreference.DARK -> true
}

@Composable
fun VarlikTakibiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
