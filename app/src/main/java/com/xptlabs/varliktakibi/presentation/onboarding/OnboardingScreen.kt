package com.xptlabs.varliktakibi.presentation.onboarding

import android.Manifest
import android.os.Build
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
    var showNotificationPermission by remember { mutableStateOf(false) }

    // Notification permission state (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Onboarding steps - iOS'taki gibi
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
            )
        )
    }

    // Permission check effect
    LaunchedEffect(showNotificationPermission) {
        if (showNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (notificationPermissionState?.status?.isGranted == true) {
                    // Permission already granted, complete onboarding
                    viewModel.setOnboardingCompleted()
                    onOnboardingComplete()
                }
            } else {
                // No permission needed for older versions
                viewModel.setOnboardingCompleted()
                onOnboardingComplete()
            }
        }
    }

    if (showNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Show notification permission screen
        NotificationPermissionScreen(
            onPermissionResult = { granted ->
                viewModel.setOnboardingCompleted()
                onOnboardingComplete()
            },
            permissionState = notificationPermissionState!!
        )
    } else {
        // Show main onboarding steps
        OnboardingStepScreen(
            step = steps[currentStep],
            currentStep = currentStep,
            totalSteps = steps.size,
            onNext = {
                if (currentStep < steps.size - 1) {
                    currentStep++
                } else {
                    // Last step - check if we need notification permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        showNotificationPermission = true
                    } else {
                        viewModel.setOnboardingCompleted()
                        onOnboardingComplete()
                    }
                }
            },
            onSkip = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showNotificationPermission = true
                } else {
                    viewModel.setOnboardingCompleted()
                    onOnboardingComplete()
                }
            },
            isLastStep = currentStep == steps.size - 1
        )
    }
}

@Composable
private fun OnboardingStepScreen(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
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

            // Icon with gradient background - iOS style
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

            Spacer(modifier = Modifier.height(60.dp))

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
                    text = if (isLastStep) "Devam Et" else "Devam Et",
                    onClick = onNext,
                    gradient = Brush.horizontalGradient(step.gradientColors)
                )

                if (!isLastStep) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Atla",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionScreen(
    onPermissionResult: (Boolean) -> Unit,
    permissionState: com.google.accompanist.permissions.PermissionState
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
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
            modifier = Modifier.padding(24.dp)
        ) {
            // Icon
            IconWithBackground(
                icon = Icons.Default.Notifications,
                contentDescription = "Notifications",
                size = 80.dp,
                iconSize = 40.dp,
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Content
            Text(
                text = "Bildirim İzni",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Varlıklarınızı takip etmenizi hatırlatmak ve güncel kur bilgileri için bildirim gönderebilir miyiz?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Permission benefits
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

            Spacer(modifier = Modifier.height(40.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientButton(
                    text = "İzin Ver",
                    onClick = {
                        if (permissionState.status.shouldShowRationale) {
                            // Show rationale
                            permissionState.launchPermissionRequest()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    }
                )

                TextButton(
                    onClick = { onPermissionResult(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Şimdi Değil")
                }
            }
        }
    }

    // Handle permission result
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            onPermissionResult(true)
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