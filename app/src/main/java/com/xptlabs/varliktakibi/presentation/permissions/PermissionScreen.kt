package com.xptlabs.varliktakibi.presentation.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    var permissionGranted by remember { mutableStateOf(false) }

    // Manual permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("PermissionScreen", "Permission result: $isGranted")
        if (isGranted) {
            permissionGranted = true
            onPermissionsGranted()
        } else {
            // İzin reddedildi, ayarlara yönlendir
            showSettingsDialog = true
        }
    }

    // Android 13+ notification permission (backup)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            onPermissionResult = { isGranted ->
                android.util.Log.d("PermissionScreen", "Accompanist permission result: $isGranted")
                if (isGranted) {
                    onPermissionsGranted()
                } else {
                    showSettingsDialog = true
                }
            }
        )
    } else null

    // Permission check
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    // Navigate when permissions are granted
    LaunchedEffect(hasNotificationPermission) {
        if (hasNotificationPermission) {
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
                icon = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Notifications,
                contentDescription = "Permissions",
                size = 80.dp,
                iconSize = 40.dp,
                colors = if (hasNotificationPermission) {
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
                    text = if (hasNotificationPermission) "İzinler Verildi!" else "Bildirim İzni",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (hasNotificationPermission) {
                        "Tüm izinler başarıyla verildi. Artık uygulamayı kullanmaya başlayabilirsiniz."
                    } else {
                        "Varlıklarınızı takip etmenizi hatırlatmak ve güncel kur bilgileri için bildirim gönderebilir miyiz?"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Permission Benefits Card
            if (!hasNotificationPermission) {
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
                        Text(
                            text = "Bildirimlerle neler yapabilirsiniz:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        PermissionBenefit(
                            icon = Icons.Default.Notifications,
                            title = "Bildirimler",
                            description = "Güncel kur bilgilendirmeleri ve portföy güncellemeleri",
                            isGranted = hasNotificationPermission
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!hasNotificationPermission) {
                    GradientButton(
                        text = "İzin Ver",
                        onClick = {
                            android.util.Log.d("PermissionScreen", "Permission button clicked - manual launcher")

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (hasNotificationPermission) {
                                    onPermissionsGranted()
                                } else {
                                    try {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } catch (e: Exception) {
                                        android.util.Log.e("PermissionScreen", "Failed to launch permission: ${e.message}")
                                        showSettingsDialog = true
                                    }
                                }
                            } else {
                                onPermissionsGranted()
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
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PermissionScreen", "Failed to launch permission from dialog: ${e.message}")
                    showSettingsDialog = true
                }
            },
            onDismiss = {
                showPermissionDialog = false
                onSkip()
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
                onSkip()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Bildirim İzni Gerekli")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bildirimleri etkinleştirmek için uygulama ayarlarından bildirim iznini manuel olarak açmanız gerekiyor.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Adım adım rehber
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Adımlar:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "1. 'Ayarlara Git' butonuna basın",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "2. 'Bildirimler' seçeneğini bulun",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "3. Bildirimleri 'Açık' konuma getirin",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "4. Uygulamaya geri dönün",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        PermissionHelper.openNotificationSettings(context)
                        onSkip()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        onSkip()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Şimdilik Atla")
                }
            }
        )
    }
}

@Composable
private fun PermissionBenefit(
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