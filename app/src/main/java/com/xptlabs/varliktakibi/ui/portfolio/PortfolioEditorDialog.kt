package com.xptlabs.varliktakibi.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.data.local.entity.color

/** Portföy oluşturma/düzenleme: ad + 6 renkli palet + hedef. iOS `PortfolioEditorView`. */
@Composable
fun PortfolioEditorDialog(
    mode: PortfolioEditorMode,
    onSave: (name: String, color: PortfolioColor, target: Double) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val editing = mode as? PortfolioEditorMode.Edit

    var name by remember { mutableStateOf(editing?.portfolio?.name.orEmpty()) }
    var selectedColor by remember {
        mutableStateOf(editing?.portfolio?.color ?: PortfolioColor.BLUE)
    }
    // ponytail: alanda binlik ayracı yok — ham rakam girilir, okunabilirliği
    // alanın altındaki önizleme sağlar (iOS'ta canlı format tuşları eziyordu).
    var targetText by remember {
        mutableStateOf(
            editing?.portfolio?.targetValue?.takeIf { it > 0 }?.toLong()?.toString().orEmpty()
        )
    }
    /** Boş bırakmak hedefi kaldırır, o yüzden 0 geçerli bir değer. */
    val parsedTarget = targetText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "Yeni Portföy" else "Portföyü Düzenle") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Portföy adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PortfolioColor.entries.forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { selectedColor = color }
                        )
                    }
                }

                androidx.compose.foundation.layout.Column {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { new -> targetText = new.filter(Char::isDigit).take(15) },
                        label = { Text("Hedef (opsiyonel)") },
                        prefix = { Text("₺ ") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (parsedTarget > 0) {
                        Text(
                            text = TrFormat.money(parsedTarget),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                if (editing != null) {
                    TextButton(onClick = onDelete) {
                        Text("Portföyü Sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), selectedColor, parsedTarget) },
                enabled = name.isNotBlank()
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
private fun ColorSwatch(
    color: PortfolioColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
