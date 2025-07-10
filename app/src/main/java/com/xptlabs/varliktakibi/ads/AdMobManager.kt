package com.xptlabs.varliktakibi.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: FirebaseAnalyticsManager
) : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "AdMobManager"
        private const val AD_UNIT_ID_APP_OPEN = BuildConfig.ADMOB_APP_OPEN_ID
        private const val AD_UNIT_ID_BANNER = BuildConfig.ADMOB_BANNER_ID
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var wasInBackground = false
    private var isInitialized = false
    private var shouldShowAppOpenAd = true

    private val _isAdMobInitialized = MutableStateFlow(false)
    val isAdMobInitialized: StateFlow<Boolean> = _isAdMobInitialized

    private val _appOpenAdLoaded = MutableStateFlow(false)
    val appOpenAdLoaded: StateFlow<Boolean> = _appOpenAdLoaded

    init {
        initializeMobileAds()
    }

    private fun initializeMobileAds() {
        Log.d(TAG, "Initializing Mobile Ads SDK")
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "Mobile Ads SDK initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true
            _isAdMobInitialized.value = true

            // Load app open ad after initialization
            scope.launch {
                // Small delay to ensure initialization is complete
                delay(1000)
                loadAppOpenAd()
            }

            // Log AdMob initialization
            analyticsManager.logCustomEvent(
                eventName = "admob_initialized",
                parameters = mapOf(
                    "initialization_status" to initializationStatus.adapterStatusMap.toString(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    private fun loadAppOpenAd() {
        if (isLoadingAd || isAppOpenAdAvailable()) {
            Log.d(TAG, "App open ad already loading or available")
            return
        }

        if (!isInitialized) {
            Log.d(TAG, "AdMob not initialized yet, skipping app open ad load")
            return
        }

        Log.d(TAG, "Loading app open ad with unit ID: $AD_UNIT_ID_APP_OPEN")
        isLoadingAd = true

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            AD_UNIT_ID_APP_OPEN,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    _appOpenAdLoaded.value = true

                    // Analytics
                    analyticsManager.logAdLoaded("app_open", AD_UNIT_ID_APP_OPEN)

                    // Show ad immediately if activity is ready
                    if (shouldShowAppOpenAd && currentActivity != null && !isShowingAd) {
                        showAppOpenAdIfAvailable()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: ${loadAdError.message}, code: ${loadAdError.code}")
                    isLoadingAd = false
                    _appOpenAdLoaded.value = false

                    // Analytics
                    analyticsManager.logAdFailedToLoad(
                        "app_open",
                        AD_UNIT_ID_APP_OPEN,
                        loadAdError.code,
                        loadAdError.message
                    )
                }
            }
        )
    }

    private fun showAppOpenAdIfAvailable() {
        if (!isAppOpenAdAvailable() || isShowingAd) {
            Log.d(TAG, "App open ad not available or already showing")
            return
        }

        val currentActivity = this.currentActivity
        if (currentActivity == null) {
            Log.d(TAG, "No current activity to show app open ad")
            return
        }

        Log.d(TAG, "Showing app open ad")
        isShowingAd = true
        shouldShowAppOpenAd = false // Don't show again until next app start

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed")
                appOpenAd = null
                isShowingAd = false
                _appOpenAdLoaded.value = false

                // Analytics
                analyticsManager.logAdClosed("app_open", AD_UNIT_ID_APP_OPEN)

                // Load next ad for next app start
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App open ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                _appOpenAdLoaded.value = false

                // Analytics
                analyticsManager.logError(
                    errorType = "app_open_ad_show_failed",
                    errorMessage = adError.message
                )

                // Load next ad
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed")

                // Analytics
                analyticsManager.logAdShown("app_open", AD_UNIT_ID_APP_OPEN)
                analyticsManager.logAppOpenAdShown()
            }
        }

        appOpenAd?.show(currentActivity)
    }

    private fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    // Activity lifecycle callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not needed for our use case
    }

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }

        // Show app open ad if coming from background
        if (wasInBackground && !isShowingAd && shouldShowAppOpenAd) {
            Log.d(TAG, "App coming from background, showing app open ad")
            shouldShowAppOpenAd = true
            showAppOpenAdIfAvailable()
        }
        wasInBackground = false
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityStopped(activity: Activity) {
        wasInBackground = true
    }

    override fun onActivityPaused(activity: Activity) {
        // Not needed for our use case
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not needed for our use case
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    // Public methods
    fun registerActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun preloadAppOpenAd() {
        scope.launch {
            loadAppOpenAd()
        }
    }

    fun showAppOpenAd(activity: Activity) {
        currentActivity = activity
        shouldShowAppOpenAd = true
        Log.d(TAG, "Manual show app open ad requested")

        // If ad is already loaded, show it immediately
        if (isAppOpenAdAvailable()) {
            showAppOpenAdIfAvailable()
        } else {
            // Load ad and show when ready
            loadAppOpenAd()
        }
    }

    fun isAdMobReady(): Boolean = _isAdMobInitialized.value

    fun createBannerAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    fun getBannerAdUnitId(): String {
        return AD_UNIT_ID_BANNER
    }

    fun getAppOpenAdUnitId(): String {
        return AD_UNIT_ID_APP_OPEN
    }

    fun logBannerAdEvent(event: String, screenName: String) {
        when (event) {
            "loaded" -> {
                analyticsManager.logAdLoaded("banner", AD_UNIT_ID_BANNER)
                analyticsManager.logBannerAdShown(screenName)
            }
            "clicked" -> {
                analyticsManager.logAdClicked("banner", AD_UNIT_ID_BANNER)
            }
            "failed_to_load" -> {
                // This will be handled in the banner composable with error details
            }
        }
    }
}