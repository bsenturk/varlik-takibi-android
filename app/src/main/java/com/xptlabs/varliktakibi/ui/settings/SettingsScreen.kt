package com.xptlabs.varliktakibi.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.data.prefs.DarkModePreference
import com.xptlabs.varliktakibi.ui.theme.AppColors

private const val PRIVACY_URL = "https://bsenturk.github.io/varliktakibi-legal/privacy.html"
private const val TERMS_URL = "https://bsenturk.github.io/varliktakibi-legal/terms.html"
private const val SUPPORT_EMAIL = "buraksenturktr@icloud.com"

@Composable
fun SettingsScreen(
    onOpenPaywall: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshNotificationStatus() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ayarlar",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (!state.isPro) {
            item { ProBanner(onClick = onOpenPaywall) }
        }

        item {
            Section("Üyelik") {
                SettingsRow(
                    icon = if (state.isPro) Icons.Filled.WorkspacePremium else Icons.Filled.Person,
                    tintHex = if (state.isPro) "#AF52DE" else "#8E8E93",
                    title = "Üyelik Durumu",
                    value = if (state.isPro) "Pro" else "Ücretsiz",
                    onClick = if (state.isPro) null else onOpenPaywall
                )
                if (!state.isPro) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    var restoreMessage by remember { mutableStateOf<String?>(null) }
                    SettingsRow(
                        icon = Icons.Filled.Check,
                        tintHex = "#34C759",
                        title = "Satın Alımları Geri Yükle",
                        value = restoreMessage,
                        onClick = {
                            viewModel.restorePurchases { restored ->
                                restoreMessage =
                                    if (restored) "Geri yüklendi" else "Abonelik bulunamadı"
                            }
                        }
                    )
                }
            }
        }

        item {
            Section("Tercihler") {
                CurrencyRow(selected = state.currency, onSelect = viewModel::setCurrency)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                DarkModeRow(selected = state.darkMode, onSelect = viewModel::setDarkMode)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    tintHex = "#FF3B30",
                    title = "Bildirimler",
                    value = if (state.notificationsEnabled) "Açık" else "Kapalı",
                    // Android'de izin bir kez reddedildikten sonra ancak sistem
                    // ayarlarından açılabiliyor; doğrudan oraya götürüyoruz.
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                )
            }
        }

        item {
            Section("Destek") {
                SettingsRow(
                    icon = Icons.Filled.Mail,
                    tintHex = "#34C759",
                    title = "Bize Ulaşın",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, "mailto:$SUPPORT_EMAIL".toUri()).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "Varlık Takibi geri bildirim")
                            }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = Icons.Filled.Star,
                    tintHex = "#FF9F0A",
                    title = "Uygulamayı Puanla",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=${context.packageName}".toUri()
                            )
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = Icons.Filled.Shield,
                    tintHex = "#8E8E93",
                    title = "Gizlilik Politikası",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = Icons.Filled.Description,
                    tintHex = "#8E8E93",
                    title = "Kullanım Koşulları",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
                    }
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.versionLabel,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "© 2026 Varlık Takibi. Tüm hakları saklıdır.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProBanner(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        "#0A84FF".toColor().copy(alpha = 0.12f),
                        "#AF52DE".toColor().copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "Pro'ya geç", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Sınırsız portföy · TEFAS · Reklamsız",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tintHex: String,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tintHex.toColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(text = title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        value?.let {
            Text(text = it, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
        if (onClick != null && trailing == null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CurrencyRow(selected: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SettingsRow(
            icon = Icons.Filled.Payments,
            tintHex = "#0A84FF",
            title = "Para Birimi",
            value = "${selected.code} (${selected.symbol})",
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.displayName) },
                    trailingIcon = {
                        if (currency == selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    }
                )
            }
        }
    }
}

@Composable
private fun DarkModeRow(
    selected: DarkModePreference,
    onSelect: (DarkModePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SettingsRow(
            icon = Icons.Filled.DarkMode,
            tintHex = "#AF52DE",
            title = "Görünüm",
            value = selected.displayName,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DarkModePreference.entries.forEach { preference ->
                DropdownMenuItem(
                    text = { Text(preference.displayName) },
                    trailingIcon = {
                        if (preference == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(preference)
                    }
                )
            }
        }
    }
}
