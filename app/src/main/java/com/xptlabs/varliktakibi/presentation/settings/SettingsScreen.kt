package com.xptlabs.varliktakibi.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.settings.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Settings items - moved inside composable
    val settingsItems = remember {
        listOf(
            SettingsItem(
                type = SettingsItemType.DARK_MODE,
                icon = Icons.Default.Palette,
                iconColor = Color(0xFF6366F1), // Indigo
                title = "Görünüm Modu",
                subtitle = "Açık/koyu tema ayarları"
            ),
            SettingsItem(
                type = SettingsItemType.NOTIFICATIONS,
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFF8B5CF6), // Purple
                title = "Bildirim Tercihleri",
                subtitle = "Bildirim tercihlerinizi yönetin"
            ),
            SettingsItem(
                type = SettingsItemType.RATE_APP,
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFFC107), // Yellow
                title = "Uygulamaya Puan Ver",
                subtitle = "Play Store'da değerlendirin"
            ),
            SettingsItem(
                type = SettingsItemType.FEEDBACK,
                icon = Icons.Default.Email,
                iconColor = Color(0xFF2196F3), // Blue
                title = "Geri Bildirim Gönder",
                subtitle = "Görüş ve önerilerinizi paylaşın"
            ),
            SettingsItem(
                type = SettingsItemType.SHARE,
                icon = Icons.Default.Share,
                iconColor = Color(0xFF4CAF50), // Green
                title = "Uygulamayı Paylaş",
                subtitle = "Arkadaşlarınızla paylaşın"
            ),
            SettingsItem(
                type = SettingsItemType.PRIVACY,
                icon = Icons.Default.Shield,
                iconColor = Color(0xFF757575), // Gray
                title = "Gizlilik Politikası",
                subtitle = "Verileriniz nasıl korunur"
            )
        )
    }

    // Dialog states
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showRateAppDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Debug section only in debug builds
            if (BuildConfig.DEBUG) {
                item {
                    DebugSection(viewModel = viewModel)
                }
            }

            // Settings items
            items(settingsItems) { item ->
                SettingsItemCard(
                    item = item,
                    onClick = {
                        when (item.type) {
                            SettingsItemType.DARK_MODE -> showDarkModeDialog = true
                            SettingsItemType.NOTIFICATIONS -> viewModel.openNotificationSettings(context)
                            SettingsItemType.RATE_APP -> showRateAppDialog = true
                            SettingsItemType.FEEDBACK -> showFeedbackDialog = true
                            SettingsItemType.SHARE -> showShareDialog = true
                            SettingsItemType.PRIVACY -> showPrivacyDialog = true
                        }
                    }
                )
            }

            // App info
            item {
                AppInfoSection()
            }

            // Extra space at bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialogs
    if (showDarkModeDialog) {
        DarkModeDialog(
            currentTheme = uiState.darkModePreference,
            onThemeSelected = { theme ->
                viewModel.setDarkModePreference(theme)
                showDarkModeDialog = false
            },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    if (showRateAppDialog) {
        RateAppDialog(
            onRate = { rating ->
                if (rating > 0) {
                    viewModel.openAppStore(context)
                }
                showRateAppDialog = false
            },
            onDismiss = { showRateAppDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onSend = { category, message ->
                viewModel.sendFeedback(context, category, message)
                showFeedbackDialog = false
            },
            onDismiss = { showFeedbackDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showShareDialog) {
        ShareAppDialog(
            onShare = {
                viewModel.shareApp(context)
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(18.dp),
                    tint = item.iconColor
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Varlık Takibi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Sürüm ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "© 2024 Varlık Takibi. Tüm hakları saklıdır.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DebugSection(viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🐛 DEBUG MODE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Build Type: ${BuildConfig.BUILD_TYPE}\nVersion: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.clearAllData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Data")
                }

                Button(
                    onClick = { viewModel.testAnalytics() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Analytics")
                }
            }
        }
    }
}

// Data classes and enums
data class SettingsItem(
    val type: SettingsItemType,
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String
)

enum class SettingsItemType {
    DARK_MODE,
    NOTIFICATIONS,
    RATE_APP,
    FEEDBACK,
    SHARE,
    PRIVACY
}

enum class DarkModePreference(
    val displayName: String,
    val iconName: ImageVector,
    val description: String
) {
    SYSTEM("Sistem", Icons.Default.Smartphone, "Sistem ayarlarını takip eder"),
    LIGHT("Açık", Icons.Default.LightMode, "Her zaman açık tema kullanır"),
    DARK("Koyu", Icons.Default.DarkMode, "Her zaman koyu tema kullanır")
}