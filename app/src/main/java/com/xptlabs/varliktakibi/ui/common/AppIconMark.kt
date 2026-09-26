package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xptlabs.varliktakibi.R
import com.xptlabs.varliktakibi.core.ext.toColor

/**
 * Uygulama ikonu bir composable olarak. Uyarlanabilir ikonu `painterResource`
 * doğrudan çizemediği için `mipmap-anydpi-v26/ic_launcher.xml`'deki iki katman
 * (gradyan zemin + ön plan) burada elle birleştiriliyor.
 */
@Composable
fun AppIconMark(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 9.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(listOf("#1589FE".toColor(), "#AF53DF".toColor()))
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            // Uyarlanabilir ikonun ön planı 108dp tuvalde 72dp güvenli alana
            // çizilir; launcher'daki görüntüyle eşleşmesi için aynı oran.
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f)
        )
    }
}
