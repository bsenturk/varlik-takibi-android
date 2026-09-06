package com.xptlabs.varliktakibi.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.ui.common.AppIconMark
import com.xptlabs.varliktakibi.ui.common.AssetRow
import com.xptlabs.varliktakibi.ui.common.AssetRowItem
import com.xptlabs.varliktakibi.ui.common.BalanceCard
import com.xptlabs.varliktakibi.ui.common.SelectableChip
import kotlinx.coroutines.launch

/**
 * Onboarding: 3 tanıtım sayfası → bildirim izni → uygulama. iOS
 * `OnboardingView` portu (ATT adımı Android'de yok).
 *
 * Sayfalardaki görseller ikon değil, uygulamanın **gerçek bileşenleriyle**
 * kuruluyor (BalanceCard, AssetRow, portföy chip'leri, Dağılım kartı):
 * kullanıcı tanıtımda gördüğü ekranla uygulamaya girince birebir aynısıyla
 * karşılaşsın diye — iOS'taki yaklaşımın aynısı.
 */
private enum class OnboardingPhase { PAGES, NOTIFICATIONS }

private class OnboardingPage(
    val title: String,
    val description: String,
    val mock: @Composable () -> Unit
)

private val PAGES = listOf(
    OnboardingPage(
        title = "Tüm Varlıkların Tek Yerde",
        description = "Altın, döviz, hisse ve kriptonu tek uygulamada, canlı fiyatlarla takip et.",
        mock = { DashboardMock() }
    ),
    OnboardingPage(
        title = "Farklı Hedefler,\nFarklı Portföyler",
        description = "Emeklilik, ev peşinatı ya da günlük takip — hedeflerine göre ayrı portföyler oluştur.",
        mock = { PortfoliosMock() }
    ),
    OnboardingPage(
        title = "Performansını\nYakından Takip Et",
        description = "Dağılım grafikleri ve zaman bazlı analizlerle varlıklarının seyrini net gör.",
        mock = { AnalysisMock() }
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(OnboardingPhase.PAGES) }
    // İzin sonucu geldiğinde analitiğe hangi sayfadan/nasıl çıkıldığını yazmak
    // için tanıtım adımının sonucunu bekletiyoruz.
    var completion by remember { mutableStateOf(PAGES.size to false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
        val (reachedPage, skipped) = completion
        viewModel.complete(reachedPage = reachedPage, skipped = skipped, onDone = onComplete)
    }

    // iOS izni yalnızca hiç sorulmamışken soruyor; Android'de 13 öncesi zaten
    // çalışma zamanı izni yok, izin verilmişse de adımı göstermenin anlamı yok.
    val needsNotificationStep = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

    // Tanıtımdan çıkışın tek kapısı: "Atla" da son sayfanın butonu da buradan
    // geçiyor, böylece bildirim adımı hiçbir yolda atlanmıyor.
    val finishPages: (Int, Boolean) -> Unit = { reachedPage, skipped ->
        completion = reachedPage to skipped
        if (needsNotificationStep) {
            phase = OnboardingPhase.NOTIFICATIONS
        } else {
            viewModel.onNotificationPermissionResult(true)
            viewModel.complete(reachedPage = reachedPage, skipped = skipped, onDone = onComplete)
        }
    }

    when (phase) {
        OnboardingPhase.PAGES -> PagesStep(onFinish = finishPages)
        OnboardingPhase.NOTIFICATIONS -> NotificationStep(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

// ── Adım 1: tanıtım sayfaları ────────────────────────────────────────────────

@Composable
private fun PagesStep(onFinish: (reachedPage: Int, skipped: Boolean) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // iOS'ta "Atla" her sayfada duruyor.
            TextButton(onClick = { onFinish(pagerState.currentPage + 1, true) }) {
                Text(
                    text = "Atla",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            PageContent(PAGES[page])
        }

        PageDots(
            count = PAGES.size,
            index = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Button(
            onClick = {
                if (isLastPage) onFinish(PAGES.size, false)
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp)
        ) {
            Text(
                text = if (isLastPage) "İlk Varlığını Ekle" else "Devam Et",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .height(32.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            page.mock()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = page.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = page.description,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { i ->
            val isActive = i == index
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.5.dp)
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
}

// ── Adım 2: bildirim izni ────────────────────────────────────────────────────

@Composable
private fun NotificationStep(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            NotificationBannerMock()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Text(
                text = "Piyasa Hareketlerinden\nHaberdar Ol",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Altın, döviz ve BIST sert hareket ettiğinde anında bildirim al. " +
                    "Fiyatı sürekli kontrol etmene gerek kalmasın.",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp)
        ) {
            Text(text = "Bildirimleri Aç", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        // "İzin Verme" de sistem diyalogunu açıyor; reddi kullanıcı orada
        // veriyor, böylece izin "hiç sorulmamış" durumunda takılı kalmıyor.
        TextButton(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(44.dp)
        ) {
            Text(
                text = "İzin Verme",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .height(24.dp)
                .navigationBarsPadding()
        )
    }
}

// ── Tanıtım görselleri ───────────────────────────────────────────────────────
//
// Hepsi uygulamanın gerçek bileşenleriyle kuruluyor; tek fark verinin örnek
// olması. Bileşen değişince tanıtım da kendiliğinden güncelleniyor.

/** İlk sayfa: Portföy sekmesi — bakiye kartı + varlık satırları. */
@Composable
private fun DashboardMock() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MockBalanceCard(
            color = PortfolioColor.BLUE,
            totalValue = 1_250_430.0,
            profitLoss = 182_640.0,
            profitLossPercent = 17.08
        )
        MockAssetRow(
            category = AssetCategory.GOLD,
            subtitle = "3 varlık",
            value = 620_400.0,
            changePercent = 2.41,
            sparkline = listOf(10.0, 11.0, 10.4, 12.0, 13.0, 12.6, 14.0, 15.2)
        )
        MockAssetRow(
            category = AssetCategory.CURRENCY,
            subtitle = "2 varlık",
            value = 358_700.0,
            changePercent = 0.86,
            sparkline = listOf(14.0, 13.6, 14.2, 14.0, 14.8, 15.0, 14.7, 15.4)
        )
    }
}

/** İkinci sayfa: hedeflere göre ayrılmış portföy chip'leri. */
@Composable
private fun PortfoliosMock() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip(label = "Genel", isSelected = false, onClick = {})
            SelectableChip(
                label = "Emeklilik",
                isSelected = true,
                onClick = {},
                gradient = PortfolioColor.PURPLE.gradient
            )
            SelectableChip(
                label = "Ev Peşinatı",
                isSelected = false,
                onClick = {},
                dotColor = PortfolioColor.ORANGE.color
            )
        }
        MockBalanceCard(
            color = PortfolioColor.PURPLE,
            totalValue = 840_000.0,
            profitLoss = 96_400.0,
            profitLossPercent = 12.96
        )
    }
}

/** Üçüncü sayfa: Analiz sekmesinin dağılım kartı. */
@Composable
private fun AnalysisMock() {
    val slices = listOf(
        AssetCategory.GOLD to 46.0,
        AssetCategory.CURRENCY to 29.0,
        AssetCategory.BIST to 15.0,
        AssetCategory.CRYPTO to 10.0
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Dağılım",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${slices.size} tür",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            slices.forEach { (category, percent) ->
                Box(
                    modifier = Modifier
                        .weight(percent.toFloat())
                        .fillMaxSize()
                        .background(category.tintHex.toColor())
                )
            }
        }

        slices.forEach { (category, percent) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(category.tintHex.toColor())
                )
                Text(
                    text = category.displayName,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TrFormat.slicePercent(percent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Bildirim adımı: sunucunun gönderdiği gerçek bildirimin banner görünümü. */
@Composable
private fun NotificationBannerMock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIconMark(modifier = Modifier.size(38.dp))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VARLIK TAKİBİ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "şimdi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Piyasalarda hareketlilik",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "📈 Piyasalarda hareketlilik var, portföyünüzü kontrol etmeyi unutmayın.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Örnek veri yardımcıları ──────────────────────────────────────────────────

@Composable
private fun MockBalanceCard(
    color: PortfolioColor,
    totalValue: Double,
    profitLoss: Double,
    profitLossPercent: Double
) {
    BalanceCard(
        portfolioColor = color,
        metrics = PortfolioMetrics(
            totalValue = totalValue,
            totalCost = totalValue - profitLoss,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent,
            hasMissingPrices = false
        ),
        currency = Currency.TRY,
        onCurrencyChange = {},
        // null: tanıtımda gizleme düğmesi yok.
        valuesMasked = null,
        onToggleMask = {},
        convert = { it }
    )
}

@Composable
private fun MockAssetRow(
    category: AssetCategory,
    subtitle: String,
    value: Double,
    changePercent: Double,
    sparkline: List<Double>
) {
    AssetRow(
        item = AssetRowItem(
            id = category.name,
            title = category.displayName,
            subtitle = subtitle,
            value = value,
            changePercent = changePercent,
            sparkline = sparkline,
            icon = category.icon,
            tintHex = category.tintHex
        ),
        currency = Currency.TRY,
        convert = { it },
        valuesMasked = false
    )
}
