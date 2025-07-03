package com.xptlabs.varliktakibi.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground
import com.xptlabs.varliktakibi.presentation.components.NotificationPermissionDialog
import com.xptlabs.varliktakibi.presentation.components.PermissionDeniedDialog
import com.xptlabs.varliktakibi.utils.PermissionHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Android 13+ notification permission
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Check if all permissions are granted
    val allPermissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState?.status?.isGranted == true
    } else {
        true // No permissions needed for older versions
    }

    // Navigate when permissions are granted
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // Header Icon
            IconWithBackground(
                icon = if (allPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                contentDescription = "Permissions",
                size = 80.dp,
                iconSize = 40.dp,
                colors = if (allPermissionsGranted) {
                    listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary)
                } else {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                }
            )

            // Title & Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (allPermissionsGranted) "İzinler Verildi!" else "İzinler",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (allPermissionsGranted) {
                        "Tüm izinler başarıyla verildi. Artık uygulamayı kullanmaya başlayabilirsiniz."
                    } else {
                        "Varlık Takibi uygulamasının en iyi deneyimi sunabilmesi için aşağıdaki izinlere ihtiyacımız var."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Permission Cards
            if (!allPermissionsGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionItem(
                                icon = Icons.Default.Notifications,
                                title = "Bildirimler",
                                description = "Güncel kur bilgilendirmeleri ve portföy güncellemeleri",
                                isGranted = notificationPermissionState?.status?.isGranted == true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!allPermissionsGranted) {
                    GradientButton(
                        text = "İzinleri Ver",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (notificationPermissionState?.status?.shouldShowRationale == true) {
                                    showPermissionDialog = true
                                } else {
                                    notificationPermissionState?.launchPermissionRequest()
                                }
                            }
                        }
                    )

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Şimdilik Atla")
                    }
                } else {
                    GradientButton(
                        text = "Devam Et",
                        onClick = onPermissionsGranted
                    )
                }
            }
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        NotificationPermissionDialog(
            onRequestPermission = {
                showPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionState?.launchPermissionRequest()
                }
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        PermissionDeniedDialog(
            onOpenSettings = {
                showSettingsDialog = false
                PermissionHelper.openAppSettings(context)
            },
            onDismiss = {
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}