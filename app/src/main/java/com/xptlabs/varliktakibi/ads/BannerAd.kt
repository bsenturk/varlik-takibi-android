package com.xptlabs.varliktakibi.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener

/**
 * Uyarlanabilir banner. Genişliğe göre yükseklik hesaplayan "anchored adaptive"
 * boyutu kullanılıyor — sabit 320x50'ye göre hem daha çok gelir hem tablette
 * daha az garip duruyor.
 */
@Composable
fun BannerAd(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val sdkReady by adMobManager.isInitialized.collectAsStateWithLifecycle()

    // Mobile Ads SDK kurulmadan yapılan loadAd() sessizce düşüyor; istek
    // remember bloğunda olduğu için de bir daha denenmiyordu. Temiz kurulumda
    // SDK'nın ilk açılışı yavaş olduğundan banner hiç yüklenmiyordu.
    if (!sdkReady) return

    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            )
            adUnitId = adMobManager.bannerAdUnitId()
            adListener = object : AdListener() {
                override fun onAdLoaded() = adMobManager.onBannerEvent("loaded")
                override fun onAdImpression() = adMobManager.onBannerEvent("impression")
                override fun onAdClicked() = adMobManager.onBannerEvent("clicked")
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("BannerAd", "Banner yüklenemedi: ${error.message}")
                    adMobManager.onBannerEvent("load_failed", error.message)
                }
            }
            onPaidEventListener = OnPaidEventListener { value ->
                adMobManager.onAdRevenue(value, "banner", adUnitId.orEmpty(), responseInfo)
            }
            loadAd(adMobManager.newAdRequest())
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth(),
        onRelease = { it.destroy() }
    )
}
