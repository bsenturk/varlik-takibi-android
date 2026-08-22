package com.xptlabs.varliktakibi.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.ui.common.AssetIconTile
import com.xptlabs.varliktakibi.ui.common.DeleteConfirmDialog

/**
 * Varlık düzenleme: miktar ve birim maliyet. Burada özel keypad yerine sistem
 * klavyesi — düzenleme nadir bir eylem ve alanlar arasında geçiş gerekiyor.
 */
@Composable
fun AssetEditScreen(
    assetId: String,
    onClose: () -> Unit,
    viewModel: AssetEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(assetId) { viewModel.load(assetId) }
    LaunchedEffect(state.finished) { if (state.finished) onClose() }

    val asset = state.asset ?: return

    var amount by remember(asset.id) { mutableStateOf(TrFormat.amount(asset.amount)) }
    var cost by remember(asset.id) { mutableStateOf(TrFormat.decimal(asset.costBasis)) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetIconTile(
                icon = asset.assetType.icon,
                tintHex = asset.assetType.tintHex,
                flag = asset.assetType.flag
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = asset.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = state.marketPrice?.let { "Güncel: ${TrFormat.money(it)}" }
                        ?: "Güncel fiyat alınamadı",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Miktar (${asset.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cost,
            onValueChange = { cost = it },
            label = { Text("Ortalama alış fiyatı (birim, ₺)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Box(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.save(amount, cost) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { confirmingDelete = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Varlığı Sil", color = MaterialTheme.colorScheme.error)
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) { Text("Vazgeç") }
    }

    if (confirmingDelete) {
        DeleteConfirmDialog(
            title = "\"${asset.name}\" silinsin mi?",
            message = "Bu varlık ve fiyat geçmişi kalıcı olarak silinecek.",
            onConfirm = {
                confirmingDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmingDelete = false }
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Hata") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("Tamam") }
            }
        )
    }
}
