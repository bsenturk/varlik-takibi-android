package com.xptlabs.varliktakibi.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.ui.common.ScreenNavBar
import java.util.Locale

private enum class FeedbackCategory(val label: String, val icon: ImageVector, val placeholder: String) {
    GENERAL("Genel", Icons.AutoMirrored.Filled.Chat, "Aklınızdakini yazın..."),
    BUG("Hata Bildirimi", Icons.Filled.BugReport, "Ne oldu? Hangi ekranda, hangi adımlarla?"),
    FEATURE("Özellik İsteği", Icons.Filled.Lightbulb, "Hangi özelliği görmek istersiniz?"),
    OTHER("Diğer", Icons.Filled.MoreHoriz, "Mesajınızı buraya yazın...")
}

/**
 * Geri bildirim — tam ekran, kategoriler tek dokunuşluk çipler, gönder butonu
 * klavyenin hemen üstünde. Mail uygulamasıyla gönderiliyor; yoksa adres
 * kopyalanabiliyor. iOS `FeedbackView` portu.
 */
@Composable
fun FeedbackScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FeedbackCategory.GENERAL) }
    var showMailFallback by remember { mutableStateOf(false) }
    val trimmed = text.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenNavBar(title = "Bize Ulaşın", onClose = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Her mesajı okuyoruz, genellikle birkaç gün içinde dönüyoruz",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Konu")
                FeedbackCategory.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            CategoryChip(
                                category = option,
                                isSelected = option == category,
                                onClick = { category = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Mesajınız")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(category.placeholder) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Uygulama sürümü ve cihaz bilgisi mesaja eklenir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = {
                if (sendMail(context, category, trimmed)) onClose() else showMailFallback = true
            },
            enabled = trimmed.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(54.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  Mail ile Gönder", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showMailFallback) {
        AlertDialog(
            onDismissRequest = { showMailFallback = false },
            title = { Text("Mail uygulaması bulunamadı") },
            text = { Text("Mesajınızı $SUPPORT_EMAIL adresine gönderebilirsiniz.") },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("e-posta", SUPPORT_EMAIL))
                    showMailFallback = false
                }) { Text("Adresi Kopyala") }
            },
            dismissButton = {
                TextButton(onClick = { showMailFallback = false }) { Text("Tamam") }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(Locale.forLanguageTag("tr-TR")),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun CategoryChip(
    category: FeedbackCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            category.icon,
            contentDescription = null,
            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            category.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** @return false ise mail uygulaması yok — yazılan mesaj kaybolmasın, ekran açık kalır. */
private fun sendMail(context: Context, category: FeedbackCategory, message: String): Boolean {
    val body = "$message\n—\n${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · " +
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Varlık Takibi - ${category.label}")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
