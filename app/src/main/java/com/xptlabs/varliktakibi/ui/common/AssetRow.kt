package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.ui.theme.AppColors

/**
 * Portföy listesindeki bir satır: tek bir varlık ya da ("Genel"de) toplanmış
 * bir kategori. iOS `DashboardRowView` karşılığı.
 */
data class AssetRowItem(
    val id: String,
    val title: String,
    val subtitle: String,
    /** TL cinsinden değer; fiyat bilinmiyorsa null. */
    val value: Double?,
    val changePercent: Double,
    val sparkline: List<Double>,
    val icon: ImageVector,
    val tintHex: String,
    val flag: String? = null,
    /** Yalnızca tek varlık satırlarında dolu — düzenleme/silme için. */
    val assetId: String? = null
)

@Composable
fun AssetRow(
    item: AssetRowItem,
    currency: Currency,
    /** TL tutarını seçili para birimine çevirir. */
    convert: (Double) -> Double,
    valuesMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val changeColor = AppColors.forChange(item.changePercent)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssetIconTile(icon = item.icon, tintHex = item.tintHex, flag = item.flag)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Sparkline(
            values = item.sparkline,
            lineColor = changeColor,
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = when {
                    valuesMasked -> TrFormat.MASK
                    // Fiyatı bilinmeyen varlıkta 0 ₺ göstermek yerine tire —
                    // "değeri sıfır" ile "fiyat gelmedi" karışmasın.
                    item.value == null -> "—"
                    else -> TrFormat.money(convert(item.value), currency)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            Text(
                text = TrFormat.percent(item.changePercent),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = changeColor
            )
        }
    }
}
