package com.xptlabs.varliktakibi.ui.addasset

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.abs
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
import com.xptlabs.varliktakibi.ui.common.ScreenNavBar
import com.xptlabs.varliktakibi.ui.common.AssetIconTile
import com.xptlabs.varliktakibi.ui.common.DecimalInput
import com.xptlabs.varliktakibi.ui.common.LocationPicker
import com.xptlabs.varliktakibi.ui.common.ManualNameField
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
    /** Huni analitiği için: akış onboarding'den mi + butonundan mı açıldı. */
    source: String,
    onPremiumLocked: () -> Unit,
    viewModel: AddAssetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedAsMerge) {
        if (state.savedAsMerge != null) onSaved()
    }

    // Hangi yoldan kapanırsa kapansın akış başa dönsün.
    DisposableEffect(Unit) {
        viewModel.onOpened(source)
        onDispose { viewModel.resetFlow() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        ScreenNavBar(
            title = when (val step = state.step) {
                is AddAssetStep.Category -> "Varlık Ekle"
                is AddAssetStep.InstrumentList -> step.category.displayName
                is AddAssetStep.Amount -> step.instrument.category.displayName
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
                onSave = { amount, price, location, name ->
                    viewModel.save(step.instrument, amount, price, location, name)
                }
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
        // Birkaç sabit seçenekte arama kutusu gürültü.
        if (!category.isManual) OutlinedTextField(
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
            // Arama kutusu yoksa liste üst çizgiye yapışmasın.
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = if (category.isManual) 12.dp else 0.dp,
                bottom = 24.dp
            ),
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
        Text(
            text = instrument.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (instrument.type.isManual) "Değerini sen gir"
            else TrFormat.money(instrument.priceTry),
            fontSize = 14.sp,
            fontWeight = if (instrument.type.isManual) FontWeight.Normal else FontWeight.Bold,
            color = if (instrument.type.isManual) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
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

/**
 * Miktar, portföy, yer ve opsiyonel alış fiyatı. Sistem klavyesi: yalnızca alana
 * dokununca açılıyor, kapalıyken tüm alanlar tek ekranda görünüyor.
 */
@Composable
private fun AmountEntry(
    instrument: Instrument,
    marketPrice: Double,
    state: AddAssetUiState,
    onSelectPortfolio: (com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity) -> Unit,
    onSave: (amount: String, purchasePrice: String, location: String, name: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    // Ev/araba/BES: alan miktar değil, TL değer; miktar hep 1.
    val isManual = instrument.type.isManual
    val focusManager = LocalFocusManager.current
    var anyFocused by remember { mutableStateOf(false) }

    // Kriptoda 8 hane gerekiyor (0,00021 BTC), tutarda 2, diğerlerinde 4.
    val amountDecimals = when {
        isManual -> 2
        instrument.category == AssetCategory.CRYPTO -> 8
        else -> 4
    }
    val parsedAmount = amount.toDoubleOrNullTr() ?: 0.0
    val parsedCost = purchasePrice.toDoubleOrNullTr()
    val isTRY = instrument.symbol == "TRY"
    val canSave = parsedAmount > 0 && state.selectedPortfolio != null

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .onFocusChanged { anyFocused = it.hasFocus },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                AssetIconTile(
                    icon = instrument.type.icon,
                    tintHex = instrument.type.tintHex,
                    flag = instrument.flag,
                    size = 34.dp
                )
                Text(
                    instrument.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BigAmountField(
                label = if (isManual) "Güncel Değer" else "Miktar",
                value = amount,
                onValueChange = { amount = DecimalInput.sanitize(it, amountDecimals) },
                suffix = if (isManual) "₺" else instrument.unit,
                // Büyük ham sayılar ("5000000") tek bakışta okunmuyor.
                preview = if (isManual && parsedAmount > 0) TrFormat.money(parsedAmount) else null
            )

            PortfolioPicker(
                portfolios = state.portfolios,
                selected = state.selectedPortfolio,
                onSelect = onSelectPortfolio
            )

            if (isManual) {
                ManualNameField(
                    name = customName,
                    onNameChange = { customName = it },
                    example = instrument.type.manualNameExample
                )
                PurchasePriceField(
                    label = instrument.type.manualCostLabel,
                    value = purchasePrice,
                    onValueChange = { purchasePrice = DecimalInput.sanitize(it, 2) },
                    placeholder = "Güncel değer",
                    hint = "Belirtmezseniz kâr/zarar hesaplanmaz."
                )
                if (parsedAmount > 0 && parsedCost != null && parsedCost > 0) {
                    ProfitLossPreview(
                        value = parsedAmount - parsedCost,
                        percent = (parsedAmount - parsedCost) / parsedCost * 100.0
                    )
                }
            } else LocationPicker(
                location = location,
                onLocationChange = { location = it },
                suggestions = instrument.category.locationSuggestions
            )

            // Türk Lirası'nın alış kuru yok (baz para birimi).
            if (!isTRY && !isManual) {
                PurchasePriceField(
                    label = if (instrument.category.isDynamic) "Ortalama Maliyet" else "Satın Alınan Kur",
                    value = purchasePrice,
                    onValueChange = { purchasePrice = DecimalInput.sanitize(it, 2) },
                    placeholder = "Güncel fiyat",
                    hint = "Belirtmezseniz güncel fiyattan alınmış kabul edilir."
                )
                LiveValuePreview(
                    unitPrice = marketPrice,
                    unit = instrument.unit,
                    amount = parsedAmount,
                    showsUnitPrice = !instrument.category.isDynamic
                )
                if (parsedAmount > 0 && parsedCost != null && parsedCost > 0 && marketPrice > 0) {
                    ProfitLossPreview(
                        value = (marketPrice - parsedCost) * parsedAmount,
                        percent = (marketPrice - parsedCost) / parsedCost * 100.0
                    )
                }
            }
            Box(modifier = Modifier.height(8.dp))
        }

        // Alt çubuk kaydırma alanının dışında: klavye açılınca odaklanan alan
        // Kaydet'in altında kalmasın.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (anyFocused) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { focusManager.clearFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.KeyboardHide, contentDescription = "Klavyeyi kapat")
                }
            }
            GradientButton(
                text = "Kaydet",
                enabled = canSave,
                onClick = { onSave(amount, purchasePrice, location, customName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Ortada büyük miktar alanı; altında odakta renklenen çizgi. */
@Composable
private fun BigAmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    preview: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val style = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 30.dp, max = 260.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text("0", style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        inner()
                    }
                }
            )
            Text(
                suffix,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(2.dp)
                .background(
                    if (focused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                )
        )
        // Büyük ham sayılar ("5000000") tek bakışta okunmuyor.
        if (preview != null) {
            Text(
                preview,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Opsiyonel maliyet: hisse/kripto/fonda "ortalama maliyet", altın/dövizde "kur". */
@Composable
private fun PurchasePriceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hint: String
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    2.dp,
                    if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .clickable { focusRequester.requestFocus() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("(Opsiyonel)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val style = TextStyle(
                fontSize = 16.sp,
                fontWeight = if (value.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            if (value.isNotEmpty()) Text("₺", style = style)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 20.dp, max = 180.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        inner()
                    }
                }
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Birim fiyat (grafik kartı yoksa) ve girilen miktarın toplam değeri. */
@Composable
private fun LiveValuePreview(unitPrice: Double, unit: String, amount: Double, showsUnitPrice: Boolean) {
    if (unitPrice <= 0) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        if (showsUnitPrice) {
            Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Güncel Fiyat", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${TrFormat.money(unitPrice)} / $unit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Toplam Değer", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(TrFormat.money(amount * unitPrice), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ProfitLossPreview(value: Double, percent: Double) {
    val color = AppColors.forChange(value)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (value >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text("Tahmini Kâr/Zarar:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${if (value >= 0) "+" else "-"}${TrFormat.money(abs(value))} (%${TrFormat.decimal(abs(percent))})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    if (enabled) listOf(Color(0xFF0A84FF), Color(0xFFAF52DE))
                    else listOf(Color.Gray, Color.Gray.copy(alpha = 0.8f))
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Portföy",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (selected != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(selected.color.color)
                )
            }
            Text(
                text = selected?.name ?: "Portföy seç",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                Icons.Filled.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
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
