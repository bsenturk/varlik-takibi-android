package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.ext.toColor

/**
 * Varlık ikonu: dövizlerde bayrak emojisi, diğerlerinde vektör ikon — iOS'ta
 * `AssetGlyph`'in SF Symbol / emoji ayrımının karşılığı.
 */
@Composable
fun AssetGlyph(
    icon: ImageVector,
    flag: String?,
    color: Color,
    size: Dp
) {
    if (flag != null) {
        Text(text = flag, fontSize = (size.value * 1.2f).sp)
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
fun AssetIconTile(
    icon: ImageVector,
    tintHex: String,
    flag: String? = null,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val tint = tintHex.toColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        AssetGlyph(icon = icon, flag = flag, color = tint, size = size * 0.42f)
    }
}
