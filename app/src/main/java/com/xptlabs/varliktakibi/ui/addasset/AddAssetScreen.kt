package com.xptlabs.varliktakibi.ui.addasset

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.data.local.entity.color
import com.xptlabs.varliktakibi.market.Instrument
import com.xptlabs.varliktakibi.ui.common.AssetIconTile
import com.xptlabs.varliktakibi.ui.common.Keypad
import com.xptlabs.varliktakibi.ui.common.KeypadInput
import com.xptlabs.varliktakibi.ui.theme.AppColors

/**
 * Üç adımlı varlık ekleme akışı: kategori → enstrüman → miktar.
 * iOS `AddAssetSheet.swift` portu.
 *
 * @param onSaved kaydetme tamam; parametre birleştirme olup olmadığını söyler
 *   (çağıran taraf interstitial/paywall kararını buna göre veriyor).
 */
@Composable
fun AddAssetScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onPremiumLocked: () -> Unit,
    viewModel: AddAssetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedAsMerge) {
        if (state.savedAsMerge != null) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        NavBar(
            title = when (val step = state.step) {
                is AddAssetStep.Category -> "Varlık Ekle"
                is AddAssetStep.InstrumentList -> step.category.displayName
                is AddAssetStep.Amount -> step.instrument.name
            },
            isFirstStep = state.step is AddAssetStep.Category,
            onBack = { if (!viewModel.goBack()) onClose() },
            onClose = onClose
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when (val step = state.step) {
            is AddAssetStep.Category -> CategoryGrid(
                isPro = state.isPro,
                onSelect = { category ->
                    if (!viewModel.openCategory(category)) onPremiumLocked()
                }
            )

            is AddAssetStep.InstrumentList -> InstrumentList(
                category = step.category,
                instruments = viewModel.filteredInstruments(),
                query = state.query,
                isSearching = state.isSearchingFunds,
                onQueryChange = viewModel::setQuery,
                onSelect = viewModel::openInstrument
            )

            is AddAssetStep.Amount -> AmountEntry(
                instrument = step.instrument,
                marketPrice = viewModel.marketPrice(step.instrument),
                state = state,
                onSelectPortfolio = viewModel::selectPortfolio,
                onSave = { amount, price -> viewModel.save(step.instrument, amount, price) }
            )
        }
    }

    state.errorMessage?.let { message ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Hata") },
            text = { Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::clearError) {
                    Text("Tamam")
                }
            }
        )
    }
}

@Composable
private fun NavBar(
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

@Composable
private fun IconCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// ── Adım 1: kategori ızgarası ────────────────────────────────────────────────

@Composable
private fun CategoryGrid(isPro: Boolean, onSelect: (AssetCategory) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(AssetCategory.entries) { category ->
            CategoryTile(
                category = category,
                locked = category.isPremium && !isPro,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun CategoryTile(category: AssetCategory, locked: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIconTile(icon = category.icon, tintHex = category.tintHex, size = 48.dp)
            if (locked) {
                Box(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Pro gerekli",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = category.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Adım 2: enstrüman listesi ────────────────────────────────────────────────

@Composable
private fun InstrumentList(
    category: AssetCategory,
    instruments: List<Instrument>,
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (Instrument) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    if (category == AssetCategory.FUND) "Fon kodu veya adı ara"
                    else "Ara"
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        )

        if (instruments.isEmpty()) {
            EmptyInstruments(category = category, isSearching = isSearching, query = query)
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(instruments, key = { it.symbol }) { instrument ->
                InstrumentRow(instrument = instrument, onClick = { onSelect(instrument) })
            }
        }
    }
}

@Composable
private fun InstrumentRow(instrument: Instrument, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssetIconTile(
            icon = instrument.type.icon,
            tintHex = instrument.type.tintHex,
            flag = instrument.flag,
            size = 38.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = instrument.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = instrument.symbol,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = TrFormat.money(instrument.priceTry),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyInstruments(category: AssetCategory, isSearching: Boolean, query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = when {
                isSearching -> "Aranıyor…"
                query.isNotBlank() -> "Sonuç bulunamadı."
                category == AssetCategory.FUND ->
                    "Fon aramak için en az 2 karakter yazın."
                else -> "Fiyatlar yükleniyor…"
            },
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Adım 3: miktar girişi ────────────────────────────────────────────────────

@Composable
private fun AmountEntry(
    instrument: Instrument,
    marketPrice: Double,
    state: AddAssetUiState,
    onSelectPortfolio: (com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity) -> Unit,
    onSave: (String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var editingPrice by remember { mutableStateOf(false) }

    // Kriptoda 8 hane gerekiyor (0,00021 BTC), tutarda 2 yeterli.
    val amountDecimals = if (instrument.category == AssetCategory.CRYPTO) 8 else 4
    val parsedAmount = amount.toDoubleOrNullTr() ?: 0.0
    val parsedCost = purchasePrice.toDoubleOrNullTr()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.height(8.dp))

        InputField(
            label = "Miktar (${instrument.unit})",
            value = amount,
            isActive = !editingPrice,
            onClick = { editingPrice = false }
        )

        InputField(
            label = "Alış fiyatı (birim, ₺) — boş bırakılırsa güncel fiyat",
            value = purchasePrice,
            isActive = editingPrice,
            placeholder = TrFormat.decimal(marketPrice),
            onClick = { editingPrice = true }
        )

        // Canlı değer ve tahmini kâr/zarar önizlemesi.
        if (parsedAmount > 0) {
            val value = parsedAmount * marketPrice
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Güncel değer: ${TrFormat.money(value)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (parsedCost != null && parsedCost > 0) {
                    val pl = (marketPrice - parsedCost) * parsedAmount
                    val plPercent = (marketPrice - parsedCost) / parsedCost * 100.0
                    Text(
                        text = "Tahmini kâr/zarar: ${TrFormat.money(pl)} (${TrFormat.percent(plPercent)})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.forChange(pl)
                    )
                }
            }
        }

        if (state.portfolios.size > 1) {
            PortfolioPicker(
                portfolios = state.portfolios,
                selected = state.selectedPortfolio,
                onSelect = onSelectPortfolio
            )
        }

        Keypad(
            onDigit = { digit ->
                if (editingPrice) {
                    purchasePrice = KeypadInput.appendDigit(purchasePrice, digit, 2)
                } else {
                    amount = KeypadInput.appendDigit(amount, digit, amountDecimals)
                }
            },
            onComma = {
                if (editingPrice) purchasePrice = KeypadInput.appendComma(purchasePrice)
                else amount = KeypadInput.appendComma(amount)
            },
            onBackspace = {
                if (editingPrice) purchasePrice = KeypadInput.backspace(purchasePrice)
                else amount = KeypadInput.backspace(amount)
            }
        )

        Button(
            onClick = { onSave(amount, purchasePrice) },
            enabled = parsedAmount > 0,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Portföye Ekle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    isActive: Boolean,
    placeholder: String = "0",
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value.ifEmpty { placeholder },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PortfolioPicker(
    portfolios: List<com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity>,
    selected: com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity?,
    onSelect: (com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.subtleFill)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(selected?.color?.color ?: MaterialTheme.colorScheme.primary)
            )
            Text(
                text = selected?.name ?: "Portföy seç",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            portfolios.forEach { portfolio ->
                DropdownMenuItem(
                    text = { Text(portfolio.name) },
                    trailingIcon = {
                        if (portfolio.id == selected?.id) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(portfolio)
                    }
                )
            }
        }
    }
}
