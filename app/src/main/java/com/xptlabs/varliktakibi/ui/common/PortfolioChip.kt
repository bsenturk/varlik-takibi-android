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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.color
import com.xptlabs.varliktakibi.ui.theme.AppColors

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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    val background = if (isSelected) {
        Modifier.background(Brush.horizontalGradient(portfolio.color.gradient), shape)
    } else {
        Modifier.background(AppColors.subtleFill, shape)
    }

    Row(
        modifier = modifier
            .clip(shape)
            .then(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!isSelected && !portfolio.isGeneral) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(portfolio.color.color)
            )
        }
        Text(
            text = portfolio.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected && !portfolio.isGeneral && showsEditPencil) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Portföyü düzenle",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
