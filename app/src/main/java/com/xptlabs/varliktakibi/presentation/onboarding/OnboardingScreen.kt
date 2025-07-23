package com.xptlabs.varliktakibi.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.xptlabs.varliktakibi.domain.models.OnboardingStep
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground
import com.xptlabs.varliktakibi.utils.PermissionHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Manual permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("OnboardingScreen", "Permission result: $isGranted")
        // İzin verilsin ya da verilmesin, onboarding'i tamamla
        viewModel.setOnboardingCompleted()
        onOnboardingComplete()
    }

    // Notification permission state (backup)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Onboarding steps
    val steps = remember {
        listOf(
            OnboardingStep(
                icon = "wallet_pass_fill",
                title = "Varlık Takibi'ne Hoş Geldiniz",
                description = "Altın, döviz ve diğer varlıklarınızı kolayca takip edin. Güncel kurlarla toplam değerinizi anında görün.",
                gradientColors = listOf(
                    Color(0xFF1976D2), // Blue
                    Color(0xFF7B1FA2)  // Purple
                )
            ),
            OnboardingStep(
                icon = "add_circle_fill",
                title = "Varlık Ekleyin",
                description = "Gram altın, çeyrek altın, dolar, euro gibi 12 farklı varlık türünü ekleyebilir ve miktarlarını girebilirsiniz.",
                gradientColors = listOf(
                    Color(0xFF4CAF50), // Green
                    Color(0xFF009688)  // Teal
                )
            ),
            OnboardingStep(
                icon = "trending_up",
                title = "Kurları Takip Edin",
                description = "Güncel alış-satış kurlarını görün. Değişim oranlarını takip ederek piyasa hareketlerini kaçırmayın.",
                gradientColors = listOf(
                    Color(0xFFFF9800), // Orange
                    Color(0xFFF44336)  // Red
                )
            ),
            // 4. step olarak notification permission ekle
            OnboardingStep(
                icon = "notifications",
                title = "Bildirim İzni",
                description = "Varlıklarınızı takip etmenizi hatırlatmak ve güncel kur bilgileri için bildirim gönderebilir miyiz?",
                gradientColors = listOf(
                    Color(0xFF9C27B0), // Purple
                    Color(0xFF673AB7)  // Deep Purple
                )
            )
        )
    }

    // Permission step kontrolü
    val isPermissionStep = currentStep == steps.size - 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    OnboardingStepScreen(
        step = steps[currentStep],
        currentStep = currentStep,
        totalSteps = steps.size,
        isPermissionStep = isPermissionStep,
        onNext = {
            if (currentStep < steps.size - 1) {
                currentStep++
            } else {
                // Son step - permission iste veya tamamla
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isPermissionStep) {
                    requestNotificationPermission(permissionLauncher, showSettingsDialog = { showSettingsDialog = true })
                } else {
                    viewModel.setOnboardingCompleted()
                    onOnboardingComplete()
                }
            }
        },
        onSkip = {
            if (currentStep == steps.size - 1) {
                // Son step'i atla
                viewModel.setOnboardingCompleted()
                onOnboardingComplete()
            } else {
                // Permission step'e atla
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    currentStep = steps.size - 1
                } else {
                    viewModel.setOnboardingCompleted()
                    onOnboardingComplete()
                }
            }
        },
        onPermissionRequest = {
            requestNotificationPermission(permissionLauncher, showSettingsDialog = { showSettingsDialog = true })
        },
        isLastStep = currentStep == steps.size - 1
    )

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
                // Dialog kapandığında onboarding'i tamamla
                viewModel.setOnboardingCompleted()
                onOnboardingComplete()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Bildirim İzni")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bildirimleri etkinleştirmek için uygulama ayarlarından bildirim iznini manuel olarak açabilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium
                    )

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
                                text = "Nasıl Açılır:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Ayarlar → Bildirimler → Açık",
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
                        // Ayarlara gittikten sonra onboarding'i tamamla
                        viewModel.setOnboardingCompleted()
                        onOnboardingComplete()
                    }
                ) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        viewModel.setOnboardingCompleted()
                        onOnboardingComplete()
                    }
                ) {
                    Text("Şimdilik Atla")
                }
            }
        )
    }
}

@Composable
private fun OnboardingStepScreen(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
    isPermissionStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onPermissionRequest: () -> Unit,
    isLastStep: Boolean
) {
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = step.gradientColors
                        )
                    )
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = step.gradientColors.first().copy(alpha = 0.3f),
                        spotColor = step.gradientColors.first().copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Map step icon to Material Icon
                val icon = when (step.icon) {
                    "wallet_pass_fill" -> Icons.Default.AccountBalanceWallet
                    "add_circle_fill" -> Icons.Default.Add
                    "trending_up" -> Icons.Default.TrendingUp
                    "notifications" -> Icons.Default.Notifications
                    else -> Icons.Default.Star
                }

                Icon(
                    imageVector = icon,
                    contentDescription = step.title,
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }

            // Permission benefits (sadece permission step'inde göster)
            if (isPermissionStep) {
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Bildirimlerle neler yapabilirsiniz:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        PermissionBenefit(
                            icon = Icons.Default.TrendingUp,
                            text = "Güncel kur bilgilendirmeleri"
                        )

                        PermissionBenefit(
                            icon = Icons.Default.AccountBalanceWallet,
                            text = "Portföy değer değişiklikleri"
                        )

                        PermissionBenefit(
                            icon = Icons.Default.Info,
                            text = "Önemli piyasa haberleri"
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Step Indicators - iOS style dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentStep) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientButton(
                    text = when {
                        isPermissionStep -> "İzin Ver"
                        isLastStep -> "Başlayalım"
                        else -> "Devam Et"
                    },
                    onClick = if (isPermissionStep) onPermissionRequest else onNext,
                    gradient = Brush.horizontalGradient(step.gradientColors)
                )

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPermissionStep) "Şimdi Değil" else "Atla",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PermissionBenefit(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Permission request helper function
private fun requestNotificationPermission(
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    showSettingsDialog: () -> Unit
) {
    android.util.Log.d("OnboardingScreen", "Requesting notification permission")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (e: Exception) {
            android.util.Log.e("OnboardingScreen", "Failed to launch permission: ${e.message}")
            showSettingsDialog()
        }
    }
}