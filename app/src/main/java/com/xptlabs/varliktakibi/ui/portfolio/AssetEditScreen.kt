package com.xptlabs.varliktakibi.ui.portfolio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.data.local.entity.category
import com.xptlabs.varliktakibi.ui.addasset.toDoubleOrNullTr
import com.xptlabs.varliktakibi.ui.common.AssetIconTile
import com.xptlabs.varliktakibi.ui.common.DeleteConfirmDialog
import com.xptlabs.varliktakibi.ui.common.LocationPicker
import com.xptlabs.varliktakibi.ui.common.ManualNameField
import com.xptlabs.varliktakibi.ui.common.ScreenNavBar
import com.xptlabs.varliktakibi.ui.theme.AppColors
import kotlin.math.abs

/**
 * Varlık düzenleme: miktar ve birim maliyet, işlem geçmişine giriş. Burada özel
 * keypad yerine sistem klavyesi — düzenleme nadir bir eylem ve alanlar arasında
 * geçiş gerekiyor. iOS `AssetEditSheet` karşılığı.
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
    // Hangi yoldan kapanırsa kapansın (X, geri, kaydet) ViewModel sıfırlansın.
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    val asset = state.asset ?: return

    // Ev/araba/BES: "Miktar" alanı TL değeri tutuyor, miktar hep 1.
    val isManual = asset.assetType.isManual
    var amount by remember(asset.id) {
        mutableStateOf(
            if (isManual) TrFormat.decimal(asset.currentPrice ?: 0.0) else TrFormat.amount(asset.amount)
        )
    }
    var cost by remember(asset.id) { mutableStateOf(TrFormat.decimal(asset.costBasis)) }
    var location by remember(asset.id) { mutableStateOf(asset.location) }
    // Tür adıyla aynıysa isim verilmemiş demektir; alan boş açılsın.
    var name by remember(asset.id) {
        mutableStateOf(if (asset.name == asset.assetType.displayName) "" else asset.name)
    }
    var confirmingDelete by remember { mutableStateOf(false) }
    var showingHistory by remember { mutableStateOf(false) }

    val currentPrice = if (isManual) amount.toDoubleOrNullTr() ?: asset.currentPrice
    else state.currentPrice
    fun money(value: Double) = if (state.valuesMasked) TrFormat.MASK else TrFormat.money(value)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenNavBar(title = "Varlığı Düzenle", onClose = onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssetIconTile(
                    icon = asset.assetType.icon,
                    tintHex = asset.assetType.tintHex,
                    flag = asset.assetType.flag,
                    logoUrl = state.logoUrl
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = asset.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(
                        text = when {
                            isManual -> asset.assetType.displayName
                            else -> state.marketPrice?.let { "Güncel: ${TrFormat.money(it)}" }
                                ?: "Güncel fiyat alınamadı"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(if (isManual) "Güncel Değer (₺)" else "Miktar (${asset.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isManual) {
                ManualNameField(
                    name = name,
                    onNameChange = { name = it },
                    example = asset.assetType.manualNameExample
                )
            }

            if (!state.isTRY) {
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    // Hisse/kripto/fon "maliyet", altın/döviz "kur" — iOS ile aynı.
                    label = {
                        Text(
                            when {
                                isManual -> "${asset.assetType.manualCostLabel} (₺)"
                                asset.category.isDynamic -> "Ortalama Maliyet (₺)"
                                else -> "Ortalama Alış Kuru (₺)"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isManual) {
                LocationPicker(
                    location = location,
                    onLocationChange = { location = it },
                    suggestions = asset.category.locationSuggestions
                )
            }

            ValuePreview(
                amount = if (isManual) 1.0 else amount.toDoubleOrNullTr() ?: 0.0,
                cost = cost.toDoubleOrNullTr() ?: asset.costBasis,
                currentPrice = currentPrice,
                showProfitLoss = !state.isTRY,
                money = ::money
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showingHistory = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "İşlem Geçmişi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Varlığı Sil",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Küçük ekranda / klavye açıkken Kaydet kaydırmanın dibinde kaybolmasın.
        Button(
            onClick = { viewModel.save(amount, cost, location, name) },
            enabled = (amount.toDoubleOrNullTr() ?: 0.0) > 0,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            Text("Kaydet", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showingHistory) {
        AssetHistoryScreen(
            asset = asset,
            transactions = state.transactions,
            currentPrice = currentPrice ?: asset.costBasis,
            isTRY = state.isTRY,
            valuesMasked = state.valuesMasked,
            logoUrl = state.logoUrl,
            onClose = { showingHistory = false }
        )
        BackHandler { showingHistory = false }
    }

    if (confirmingDelete) {
        DeleteConfirmDialog(
            title = "\"${asset.name}\" silinsin mi?",
            message = "Bu varlık ve işlem geçmişi kalıcı olarak silinecek.",
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

/** Girilen miktarın güncel değeri ve (düzenlenmiş) maliyete göre kâr/zarar. */
@Composable
private fun ValuePreview(
    amount: Double,
    cost: Double,
    currentPrice: Double?,
    showProfitLoss: Boolean,
    money: (Double) -> String
) {
    val price = currentPrice ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        PreviewRow("Güncel Değer") {
            Text(money(amount * price), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        if (showProfitLoss && cost > 0 && amount > 0) {
            val pl = (price - cost) * amount
            val pct = (price - cost) / cost * 100
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            PreviewRow("Kâr / Zarar") {
                Text(
                    "${if (pl >= 0) "+" else "-"}${money(abs(pl))} (%${TrFormat.decimal(abs(pct))})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.forChange(pl)
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        value()
    }
}
