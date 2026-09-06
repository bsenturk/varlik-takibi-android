package com.xptlabs.varliktakibi.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.ext.toColor

/** Android 12+ sistem açılış ekranının çizdiği ikonun ölçüsü. */
private val SYSTEM_SPLASH_ICON_SIZE = 160.dp

/**
 * Soğuk açılışta app-open reklamı gösterilene (ya da zaman aşımına) kadar
 * içeriği örten açılış ekranı. iOS `ColdStartSplash` portu.
 *
 * Android 12+ her uygulamaya kaldırılamayan bir sistem açılış ekranı gösteriyor
 * (ortada daireye maskelenmiş uygulama ikonu). Bu ekran onu **değiştirmiyor,
 * sürdürüyor**: aynı zemin, aynı boyutta ve konumda daire ikon; yalnızca isim
 * ve nabız altına beliriyor. Kullanıcı iki ayrı açılış ekranı değil tek bir
 * ekran görüyor.
 *
 * Altındaki içeriğe dokunuşları yutar: kapı görsel bir perde değil, gerçek bir
 * engel.
 */
@Composable
fun ColdStartSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Perdenin altındaki ekran hâlâ tıklanabilir olmasın.
            .selectable(
                selected = false,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        // Sistem açılış ekranındaki ikonla aynı yerde ve aynı boyutta: ikon
        // yerinden kıpırdamıyor, altındaki yazı belirirken tek ekran gibi
        // okunuyor.
        AppIconMark(
            cornerRadius = SYSTEM_SPLASH_ICON_SIZE / 2,
            modifier = Modifier
                // Gölge nötr gri değil, ikonun morundan tonlandı.
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = "#AF52DE".toColor(),
                    spotColor = "#AF52DE".toColor()
                )
                .size(SYSTEM_SPLASH_ICON_SIZE)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .align(Alignment.Center)
                // İkonun yarıçapı + 32'lik boşluk; ikonu ortadan kaydırmadan
                // altına yerleşiyor.
                .offset(y = SYSTEM_SPLASH_ICON_SIZE / 2 + 56.dp)
        ) {
            Text(
                text = "Varlık Takibi",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            PulsingDots()
        }
    }
}

/**
 * Dönen bir spinner yerine sakin bir nabız: "yükleniyor" der, "takıldı" demez.
 * Finansal bir uygulamada hareket bir güven sinyali.
 */
@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cold_start_dots")
    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            val pulse by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    // Noktalar aynı anda değil sırayla nabız atsın.
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(pulse)
                    .alpha(pulse)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
