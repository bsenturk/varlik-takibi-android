package com.xptlabs.varliktakibi.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

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

    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            )
            adUnitId = adMobManager.bannerAdUnitId()
            adListener = object : AdListener() {
                override fun onAdLoaded() = adMobManager.onBannerEvent("ad_loaded")
                override fun onAdClicked() = adMobManager.onBannerEvent("ad_clicked")
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("BannerAd", "Banner yüklenemedi: ${error.message}")
                    adMobManager.onBannerEvent("ad_load_failed")
                }
            }
            loadAd(adMobManager.newAdRequest())
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth()
    )
}
