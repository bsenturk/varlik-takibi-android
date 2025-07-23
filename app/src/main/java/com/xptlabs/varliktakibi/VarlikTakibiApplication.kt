package com.xptlabs.varliktakibi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.managers.AssetTrackerWorkManager
import com.xptlabs.varliktakibi.notifications.AppNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VarlikTakibiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: AssetTrackerWorkManager

    @Inject
    lateinit var analyticsManager: FirebaseAnalyticsManager

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var notificationManager: AppNotificationManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        initializeFirebase()

        // Initialize AdMob
        initializeAdMob()

        // Initialize Notifications - Bu önemli!
        initializeNotifications()

        // Setup global exception handler
        setupGlobalExceptionHandler()

        // Start background work
        setupBackgroundWork()

        // Log app start
        analyticsManager.logAppOpened("application_start")
    }

    private fun initializeFirebase() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Configure Analytics
        FirebaseAnalytics.getInstance(this).apply {
            setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        // Configure Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("version_code", BuildConfig.VERSION_CODE.toString())
        }

        // Set user properties
        setFirebaseUserProperties()
    }

    private fun initializeAdMob() {
        // Register activity lifecycle callbacks for AdMob
        adMobManager.registerActivityLifecycleCallbacks(this)

        // Preload app open ad
        adMobManager.preloadAppOpenAd()
    }

    private fun initializeNotifications() {
        // Notification channel'ı oluştur - Bu Android'e capability'yi gösterir
        notificationManager.createNotificationChannel()

        // Notification capability'sini kaydet
        notificationManager.forceRegisterNotificationCapability()

        // Schedule first notification on app start
        notificationManager.scheduleNextNotification()

        // Analytics
        analyticsManager.logCustomEvent(
            eventName = "notification_system_initialized",
            parameters = mapOf(
                "notifications_enabled" to notificationManager.areNotificationsEnabled(),
                "app_start" to true
            )
        )
    }

    private fun setFirebaseUserProperties() {
        analyticsManager.apply {
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("build_type", BuildConfig.BUILD_TYPE)
            setUserProperty("version_code", BuildConfig.VERSION_CODE.toString())
            setUserProperty("debug_build", BuildConfig.DEBUG.toString())
            setUserProperty("firebase_debug", BuildConfig.FIREBASE_DEBUG.toString())
            setUserProperty("notifications_enabled", notificationManager.areNotificationsEnabled().toString())
        }
    }

    private fun setupBackgroundWork() {
        // Start periodic sync every 15 minutes
        workManager.startPeriodicSync()

        // Start daily cleanup
        workManager.startPeriodicCleanup()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // Log to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("thread_name", thread.name)
                recordException(exception)
            }

            // Log to Analytics
            analyticsManager.logError(
                errorType = "uncaught_exception",
                errorMessage = exception.message ?: "Unknown error"
            )

            // Call default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}