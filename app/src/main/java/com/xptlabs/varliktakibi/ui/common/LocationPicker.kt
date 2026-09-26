package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.data.repo.AssetEditor

/**
 * "Nerede tutuluyor?" — serbest metin + kategoriye göre öneri çipleri.
 * Ekleme ve düzenleme ekranlarında ortak. iOS `LocationPicker` portu.
 */
@Composable
fun LocationPicker(
    location: String,
    onLocationChange: (String) -> Unit,
    suggestions: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Nerede tutuluyor?", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("(Opsiyonel)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OutlinedTextField(
            value = location,
            onValueChange = { onLocationChange(it.take(AssetEditor.MAX_LOCATION_LENGTH)) },
            placeholder = { Text("Örn. ${suggestions.take(2).joinToString(", ")}") },
            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
            trailingIcon = if (location.isNotEmpty()) {
                {
                    IconButton(onClick = { onLocationChange("") }) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Temizle")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val current = AssetEditor.normalizedLocation(location)
            suggestions.forEach { title ->
                val selected = current == title
                SelectableChip(
                    label = title,
                    isSelected = selected,
                    onClick = { onLocationChange(if (selected) "" else title) }
                )
            }
        }
    }
}
