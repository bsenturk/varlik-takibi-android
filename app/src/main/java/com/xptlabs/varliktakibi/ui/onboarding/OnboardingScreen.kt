package com.xptlabs.varliktakibi.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.ext.toColor
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradient: List<String>
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Filled.Workspaces,
        title = "Tüm Varlıkların\nTek Yerde",
        description = "Altın, döviz, kripto, hisse ve fonlarını tek portföyde topla; " +
            "değerleri canlı fiyatlarla güncellensin.",
        gradient = listOf("#0A84FF", "#5E5CE6")
    ),
    OnboardingPage(
        icon = Icons.Filled.PieChart,
        title = "Farklı Hedefler,\nFarklı Portföyler",
        description = "Emeklilik, tatil, acil durum… Her hedef için ayrı portföy aç, " +
            "karışmasın.",
        gradient = listOf("#AF52DE", "#BF5AF2")
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.ShowChart,
        title = "Performansını\nYakından Takip Et",
        description = "Kâr/zararını, dağılımını ve zaman içindeki değişimini " +
            "tek bakışta gör.",
        gradient = listOf("#34C759", "#30D158")
    ),
    OnboardingPage(
        icon = Icons.Filled.Notifications,
        title = "Piyasa Kıpırdayınca\nHaberin Olsun",
        description = "Altın, dolar ya da borsa belirgin hareket ettiğinde bildirim gönderelim. " +
            "Günde en fazla bir kez.",
        gradient = listOf("#FF9F0A", "#FF9500")
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    // Android 13+ bildirim izni: son sayfanın CTA'sında isteniyor.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
        viewModel.complete(reachedPage = PAGES.size, skipped = false)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastPage) {
                TextButton(onClick = {
                    viewModel.complete(reachedPage = pagerState.currentPage + 1, skipped = true)
                    onComplete()
                }) { Text("Atla") }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            PageContent(PAGES[page])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            PAGES.indices.forEach { index ->
                val isActive = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (isActive) 22.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Button(
            onClick = {
                if (!isLastPage) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.onNotificationPermissionResult(true)
                    viewModel.complete(reachedPage = PAGES.size, skipped = false)
                    onComplete()
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .height(54.dp)
        ) {
            Text(
                text = if (isLastPage) "Bildirimlere İzin Ver" else "Devam",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(page.gradient.map { it.toColor() })),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Box(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Box(modifier = Modifier.height(14.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}
