package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.ui.theme.AppColors

/**
 * Tam ekran sayfaların üst çubuğu: solda kapat/geri dairesi, ortada başlık.
 * iOS `SelectionScreenHeader` karşılığı.
 */
@Composable
fun ScreenNavBar(
    title: String,
    isFirstStep: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(
            icon = if (isFirstStep) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (isFirstStep) "Kapat" else "Geri",
            onClick = if (isFirstStep) onClose else onBack
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        // Başlığın gerçekten ortalanması için sağda simetrik boşluk.
        Box(modifier = Modifier.size(36.dp))
    }
}

/** Tek ekranlık sayfalar için kısa yol: yalnızca kapat düğmesi. */
@Composable
fun ScreenNavBar(title: String, onClose: () -> Unit) =
    ScreenNavBar(title = title, isFirstStep = true, onBack = onClose, onClose = onClose)

@Composable
fun IconCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AppColors.subtleFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}
