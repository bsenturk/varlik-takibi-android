package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Ev/araba/BES gibi elle girilen varlıklara isim ("Kadıköy daire"): iki ev
 * listede ikisi de "Ev" diye görünmesin. Boş bırakılırsa tür adı kullanılır.
 * iOS `ManualNameField` portu.
 */
@Composable
fun ManualNameField(name: String, onNameChange: (String) -> Unit, example: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("İsim", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("(Opsiyonel)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it.take(AssetEditor.MAX_NAME_LENGTH)) },
            placeholder = { Text("Örn. $example") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
