package com.xptlabs.varliktakibi.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.ui.theme.AppColors

private val PRO_PERKS = listOf(
    "Sınırsız portföy",
    "TEFAS yatırım fonları",
    "Reklamsız deneyim"
)

@Composable
fun PaywallScreen(
    context: PaywallContext,
    onClose: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(context) { viewModel.onShown(context) }
    LaunchedEffect(state.purchaseSucceeded) {
        if (state.purchaseSucceeded) onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.subtleFill)
                    .clickable {
                        viewModel.onDismissed(context)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Kapat",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Varlık Pro",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = context.headline,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp
            )
            Text(
                text = context.subtitle,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PRO_PERKS.forEach { perk ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(text = perk, fontSize = 16.sp)
                    }
                }
            }

            when {
                state.isUnavailable -> Text(
                    text = "Abonelikler şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error
                )

                state.plans.isEmpty() -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = plan.pkg.identifier == state.selectedPlan?.pkg?.identifier,
                            onClick = { viewModel.selectPlan(plan) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activity?.let(viewModel::purchase) },
                enabled = state.selectedPlan != null && !state.isPurchasing && activity != null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (state.isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (state.selectedPlan?.hasTrial == true) "Ücretsiz Dene"
                        else "Pro'ya Geç",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = viewModel::restore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Satın alımları geri yükle", fontSize = 14.sp)
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlanOption, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                "#0A84FF".toColor().copy(alpha = 0.12f),
                                "#AF52DE".toColor().copy(alpha = 0.12f)
                            )
                        )
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = plan.label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (plan.hasTrial) {
                Text(
                    text = "Deneme süresi dahil",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = plan.price, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "/ ${plan.periodSuffix}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
