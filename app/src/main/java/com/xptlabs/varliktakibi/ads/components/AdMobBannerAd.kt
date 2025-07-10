package com.xptlabs.varliktakibi.ads.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.ui.theme.AssetTrackerTheme

@Composable
fun AdMobBannerAd(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER,
    screenName: String = "unknown_screen",
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var adView by remember { mutableStateOf<AdView?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        Log.d("AdMobBannerAd", "Creating banner ad for screen: $screenName")

        val bannerAdView = AdView(context).apply {
            adUnitId = adMobManager.getBannerAdUnitId()
            setAdSize(adSize)

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("AdMobBannerAd", "Banner ad loaded successfully for screen: $screenName")
                    isAdLoaded = true

                    // Analytics
                    adMobManager.logBannerAdEvent("loaded", screenName)

                    onAdLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Log.e("AdMobBannerAd", "Banner ad failed to load for screen $screenName: ${error.message}")
                    isAdLoaded = false

                    // Analytics
                    adMobManager.logBannerAdEvent("failed_to_load", screenName)

                    onAdFailedToLoad?.invoke(error)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d("AdMobBannerAd", "Banner ad clicked on screen: $screenName")

                    // Analytics
                    adMobManager.logBannerAdEvent("clicked", screenName)

                    onAdClicked?.invoke()
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Log.d("AdMobBannerAd", "Banner ad opened on screen: $screenName")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.d("AdMobBannerAd", "Banner ad closed on screen: $screenName")
                }
            }
        }

        adView = bannerAdView

        // Load ad if AdMob is ready
        if (adMobManager.isAdMobReady()) {
            Log.d("AdMobBannerAd", "Loading banner ad for screen: $screenName")
            bannerAdView.loadAd(adMobManager.createBannerAdRequest())
        } else {
            Log.d("AdMobBannerAd", "AdMob not ready for screen: $screenName, waiting...")
        }

        onDispose {
            Log.d("AdMobBannerAd", "Disposing banner ad for screen: $screenName")
            bannerAdView.destroy()
            adView = null
            isAdLoaded = false
        }
    }

    // Only show the ad container if AdMob is ready
    if (adMobManager.isAdMobReady()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center
        ) {
            adView?.let { ad ->
                AndroidView(
                    factory = { ad },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AdMobSmartBannerAd(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier,
    screenName: String = "unknown_screen",
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null
) {
    AdMobBannerAd(
        adMobManager = adMobManager,
        modifier = modifier,
        adSize = AdSize.SMART_BANNER,
        screenName = screenName,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked
    )
}

@Composable
fun AdMobLargeBannerAd(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier,
    screenName: String = "unknown_screen",
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null
) {
    AdMobBannerAd(
        adMobManager = adMobManager,
        modifier = modifier,
        adSize = AdSize.LARGE_BANNER,
        screenName = screenName,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked
    )
}

@Preview(showBackground = true)
@Composable
fun AdMobBannerAdPreview() {
    AssetTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "AdMob Banner Placeholder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}