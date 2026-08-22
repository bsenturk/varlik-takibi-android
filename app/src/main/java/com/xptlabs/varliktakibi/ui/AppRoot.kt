package com.xptlabs.varliktakibi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.ads.BannerAd
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.ui.addasset.AddAssetScreen
import com.xptlabs.varliktakibi.ui.analysis.AnalysisScreen
import com.xptlabs.varliktakibi.ui.main.MainScaffold
import com.xptlabs.varliktakibi.ui.main.MainTab
import com.xptlabs.varliktakibi.ui.onboarding.OnboardingScreen
import com.xptlabs.varliktakibi.ui.paywall.PaywallContext
import com.xptlabs.varliktakibi.ui.paywall.PaywallScreen
import com.xptlabs.varliktakibi.ui.portfolio.AssetEditScreen
import com.xptlabs.varliktakibi.ui.portfolio.DashboardScreen
import com.xptlabs.varliktakibi.ui.rates.RatesScreen
import com.xptlabs.varliktakibi.ui.settings.SettingsScreen

/**
 * Uygulamanın tepe kabuğu. Navigation kütüphanesi yerine düz durum: ekran
 * sayısı dört sekme + üç tam ekran örtü kadar ve derin bağlantı yok; bir rota
 * grafiği burada işi karmaşıklaştırmaktan başka bir şey yapmazdı.
 */
@Composable
fun AppRoot(
    adMobManager: AdMobManager,
    purchaseManager: PurchaseManager,
    viewModel: AppRootViewModel = hiltViewModel()
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    when (onboardingCompleted) {
        // Tercih okunana kadar boş zemin — yanlış ekranı bir kare gösterip
        // hemen değiştirmek titremeye yol açıyor.
        null -> Box(modifier = Modifier.fillMaxSize())

        false -> OnboardingScreen(onComplete = viewModel::onOnboardingComplete)

        true -> MainContent(
            adMobManager = adMobManager,
            purchaseManager = purchaseManager,
            viewModel = viewModel
        )
    }
}

@Composable
private fun MainContent(
    adMobManager: AdMobManager,
    purchaseManager: PurchaseManager,
    viewModel: AppRootViewModel
) {
    val activity = LocalActivity.current
    val bannerVisible by adMobManager.bannerVisible.collectAsStateWithLifecycle()
    val isPro by purchaseManager.isPro.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.PORTFOLIO) }
    var showAddAsset by remember { mutableStateOf(false) }
    var editingAssetId by remember { mutableStateOf<String?>(null) }
    var paywallContext by remember { mutableStateOf<PaywallContext?>(null) }
    // Ekleme akışının kapanışını olay olarak taşıyoruz: doğrudan `showAddAsset`
    // üzerinde LaunchedEffect kurmak ilk kompozisyonda da tetikleniyor ve
    // onboarding paywall bayrağını daha ekran açılmadan tüketiyordu.
    var addAssetClosed by remember { mutableStateOf<Boolean?>(null) }

    // Onboarding devri: varlık ekleme akışını bir kez otomatik aç.
    LaunchedEffect(Unit) {
        if (viewModel.consumePendingFirstAssetAdd()) showAddAsset = true
    }

    MainScaffold(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onAddAsset = {
            adMobManager.loadInterstitialAd()
            showAddAsset = true
        },
        showBanner = bannerVisible && !isPro,
        banner = { BannerAd(adMobManager = adMobManager) }
    ) {
        when (selectedTab) {
            MainTab.PORTFOLIO -> DashboardScreen(
                onEditAsset = { editingAssetId = it },
                onPortfolioLimitReached = { paywallContext = PaywallContext.PORTFOLIO_LIMIT }
            )

            MainTab.ANALYSIS -> AnalysisScreen()
            MainTab.RATES -> RatesScreen()
            MainTab.SETTINGS -> SettingsScreen(
                onOpenPaywall = { paywallContext = PaywallContext.GENERAL }
            )
        }
    }

    // ── Tam ekran örtüler ────────────────────────────────────────────────────

    AnimatedVisibility(
        visible = showAddAsset,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        AddAssetScreen(
            onClose = {
                showAddAsset = false
                addAssetClosed = false
            },
            onSaved = {
                showAddAsset = false
                addAssetClosed = true
            },
            onPremiumLocked = { paywallContext = PaywallContext.FUND }
        )
    }

    // Ekleme akışı kapandığında reklam/paywall kararı — reklamı akışın ortasında
    // değil, bu doğal geçişte gösteriyoruz.
    LaunchedEffect(addAssetClosed) {
        val didAdd = addAssetClosed ?: return@LaunchedEffect
        addAssetClosed = null

        when (viewModel.onAddAssetClosed(didAddAsset = didAdd, isPro = isPro)) {
            AdOpportunity.PAYWALL -> paywallContext = PaywallContext.ONBOARDING
            AdOpportunity.INTERSTITIAL -> activity?.let { adMobManager.showInterstitial(it) }
            AdOpportunity.NOTHING -> Unit
        }
    }

    editingAssetId?.let { assetId ->
        AssetEditScreen(assetId = assetId, onClose = { editingAssetId = null })
        BackHandler { editingAssetId = null }
    }

    paywallContext?.let { context ->
        PaywallScreen(context = context, onClose = { paywallContext = null })
        BackHandler { paywallContext = null }
    }

    // Ekleme akışında geri tuşu adım adım geri gitsin; AddAssetScreen kendi
    // içinde yönetiyor, burada yalnızca en dış kapanış.
    BackHandler(enabled = showAddAsset && editingAssetId == null && paywallContext == null) {
        showAddAsset = false
        addAssetClosed = false
    }
}
