package com.xptlabs.varliktakibi.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionType
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.ui.common.AssetIconTile
import com.xptlabs.varliktakibi.ui.common.ScreenNavBar
import com.xptlabs.varliktakibi.ui.theme.AppColors
import com.xptlabs.varliktakibi.ui.theme.SystemBlue
import com.xptlabs.varliktakibi.ui.theme.SystemGreen
import com.xptlabs.varliktakibi.ui.theme.SystemOrange
import com.xptlabs.varliktakibi.ui.theme.SystemRed
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Bir varlığın işlem geçmişi — tam ekran, "Varlığı Düzenle"den açılıyor. Her
 * alım için: ne zaman, ne kadar, o gün kaça alındı, bugün kaç ediyor.
 *
 * "Bugün" değerleri kaydedilmiyor, canlı fiyattan her çizimde hesaplanıyor;
 * fiyat her açılışta yenilendiği için ekran kendiliğinden güncel.
 * iOS `AssetHistoryView` portu.
 */
@Composable
fun AssetHistoryScreen(
    asset: AssetEntity,
    /** Yeniden eskiye. */
    transactions: List<TransactionHistoryEntity>,
    currentPrice: Double,
    isTRY: Boolean,
    valuesMasked: Boolean,
    onClose: () -> Unit
) {
    fun money(value: Double) = if (valuesMasked) TrFormat.MASK else TrFormat.money(value)
    /** Ev/araba gibi elle girilen varlık: miktar hep 1, anlamlı olan değer. */
    val isManual = asset.assetType.isManual

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        ScreenNavBar(title = "İşlem Geçmişi", onClose = onClose)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Ne zaman, ne kadar eklendi, bugün ne ediyor",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            SummaryCard(
                asset = asset,
                currentValue = money(asset.amount * currentPrice),
                // Elle girilende fiyat = değer; aynı sayıyı iki kez yazmayalım.
                currentPrice = if (isTRY || isManual) null else TrFormat.money(currentPrice),
                firstAdded = transactions.lastOrNull()?.let { formatDate(it.date) } ?: "—"
            )

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                    Text("Henüz işlem yok", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "İşlemler",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${transactions.size} işlem",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isManual || !isTRY) {
                        Text(
                            if (isManual) "Bugünkü değer, en son girdiğin değerdir."
                            else "Bugünkü değerler güncel fiyattan hesaplanır.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        transactions.forEachIndexed { index, txn ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                            TransactionRow(
                                txn = txn,
                                unit = asset.unit,
                                currentPrice = currentPrice,
                                isTRY = isTRY,
                                isManual = isManual,
                                money = ::money
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    asset: AssetEntity,
    currentValue: String,
    currentPrice: String?,
    firstAdded: String
) {
    val type = asset.assetType
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetIconTile(icon = type.icon, tintHex = type.tintHex, flag = type.flag)
            Column {
                Text(asset.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(
                    if (asset.assetType.isManual) asset.assetType.displayName
                    else "${TrFormat.amount(asset.amount)} ${asset.unit}" +
                        if (asset.location.isEmpty()) "" else " · ${asset.location}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.Top) {
            Stat("Güncel Değer", currentValue, Alignment.Start, Modifier.weight(1f))
            if (currentPrice != null) {
                Stat("Güncel Fiyat", currentPrice, Alignment.CenterHorizontally, Modifier.weight(1f))
            }
            Stat("İlk Ekleme", firstAdded, Alignment.End, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    alignment: Alignment.Horizontal,
    modifier: Modifier
) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionRow(
    txn: TransactionHistoryEntity,
    unit: String,
    currentPrice: Double,
    isTRY: Boolean,
    isManual: Boolean,
    money: (Double) -> String
) {
    val (label, icon, tint) = txn.transactionType.style()
    val isBuy = txn.transactionType == TransactionType.INITIAL ||
        txn.transactionType == TransactionType.ADD
    val amountLine = when {
        // Elle girilende "+1 adet" anlamsız: alımda değer satırı yeterli,
        // güncellemede o gün girilen değer gösterilir.
        isManual -> if (isBuy) "" else money(txn.price)
        // Sadece maliyet düzeltmesi: miktar değişmedi, toplamı göster.
        txn.transactionType == TransactionType.EDIT && txn.amount == 0.0 ->
            "Toplam ${TrFormat.amount(txn.totalAmount)} $unit"
        else -> {
            val sign = when {
                isBuy -> "+"
                txn.transactionType == TransactionType.REMOVE -> "−"
                else -> ""
            }
            "$sign${TrFormat.amount(txn.amount)} $unit"
        }
    }

    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                formatDate(txn.date),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (amountLine.isNotEmpty()) {
                Text(amountLine, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            val secondary = MaterialTheme.colorScheme.onSurfaceVariant
            when {
                isBuy && txn.amount > 0 -> {
                    // "₺1.000 → ₺1.250  +%25" — alındığı gün ne kadardı, bugün ne kadar.
                    val then = txn.amount * txn.price
                    if (isTRY || txn.price <= 0) {
                        Text(money(then), fontSize = 12.sp, color = secondary)
                    } else {
                        val now = txn.amount * currentPrice
                        val pct = (currentPrice - txn.price) / txn.price * 100
                        Text(
                            "${money(then)} → ${money(now)}",
                            fontSize = 12.sp,
                            color = secondary,
                            maxLines = 1
                        )
                        Text(
                            "${if (pct >= 0) "+" else "−"}%${TrFormat.decimal(abs(pct))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.forChange(pct)
                        )
                    }
                }
                txn.transactionType == TransactionType.REMOVE && txn.amount > 0 && !isTRY ->
                    Text("${TrFormat.money(txn.price)} / $unit", fontSize = 12.sp, color = secondary)
            }
        }
    }
}

private fun TransactionType.style(): Triple<String, ImageVector, Color> = when (this) {
    TransactionType.INITIAL -> Triple("İlk Ekleme", Icons.Filled.Star, SystemBlue)
    TransactionType.ADD -> Triple("Ekleme", Icons.Filled.AddCircle, SystemGreen)
    TransactionType.REMOVE -> Triple("Çıkarma", Icons.Filled.RemoveCircle, SystemRed)
    TransactionType.EDIT -> Triple("Güncelleme", Icons.Filled.Edit, SystemOrange)
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR"))

private fun formatDate(epochMillis: Long): String =
    Days.toLocalDate(Days.startOf(epochMillis)).format(dateFormatter)
