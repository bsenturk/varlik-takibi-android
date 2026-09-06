package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.color
import com.xptlabs.varliktakibi.ui.theme.AppColors

/**
 * Uygulamanın tek seçim hapı. Seçiliyken gradyanla dolup yazısı beyaz olur,
 * seçili değilken nötr zeminde durur.
 *
 * Görünüm tek yerde tanımlı: portföy chip'i ve Piyasalar sekmeleri buradan
 * besleniyor, böylece iki ekran zamanla birbirinden ayrılmıyor.
 */
@Composable
fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: List<Color> = PortfolioColor.BLUE.gradient,
    /** Seçili değilken yazının önünde gösterilen renk noktası. */
    dotColor: Color? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    /** Pro bitince ücretsiz sınırın dışında kalan portföy: önünde kilit durur. */
    isLocked: Boolean = false
) {
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) Brush.horizontalGradient(gradient)
                else Brush.horizontalGradient(listOf(AppColors.subtleFill, AppColors.subtleFill)),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!isSelected && dotColor != null && !isLocked) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        if (isLocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = if (isSelected) Color.White else AppColors.pro,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected && trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingContentDescription,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Portföy seçme hapı. Seçiliyken portföyün gradyanıyla dolar; seçili değilse
 * rengi öndeki küçük noktayla belli eder. iOS `PortfolioChip` portu.
 */
@Composable
fun PortfolioChip(
    portfolio: PortfolioEntity,
    isSelected: Boolean,
    /** Seçili chip'e tekrar dokunmak düzenleme açıyorsa kalem gösterilir. */
    showsEditPencil: Boolean = true,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableChip(
        label = portfolio.name,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        isLocked = isLocked,
        gradient = portfolio.color.gradient,
        // "Genel" bir toplayıcı, kendi rengi yok.
        dotColor = if (portfolio.isGeneral) null else portfolio.color.color,
        trailingIcon = if (!portfolio.isGeneral && showsEditPencil && !isLocked) Icons.Filled.Edit
        else null,
        trailingContentDescription = "Portföyü düzenle"
    )
}
