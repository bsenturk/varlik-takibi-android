package com.xptlabs.varliktakibi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.managers.AssetTrackerWorkManager
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        initializeFirebase()

        // Start background work
        setupBackgroundWork()

        // Log app start
        analyticsManager.logAppOpened("application_start")
    }

    private fun initializeFirebase() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable/disable analytics collection based on build type
        FirebaseAnalytics.getInstance(this).apply {
            setAnalyticsCollectionEnabled(!BuildConfig.DEBUG || BuildConfig.FIREBASE_DEBUG)
        }

        // Configure Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        }

        // Configure Performance Monitoring
        FirebasePerformance.getInstance().apply {
            isPerformanceCollectionEnabled = !BuildConfig.DEBUG
        }

        // Set user properties
        setFirebaseUserProperties()
    }

    private fun setFirebaseUserProperties() {
        analyticsManager.apply {
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("build_type", BuildConfig.BUILD_TYPE)
            setUserProperty("debug_build", BuildConfig.DEBUG.toString())
        }
    }

    private fun setupBackgroundWork() {
        // Start periodic sync every 15 minutes
        workManager.startPeriodicSync()

        // Start daily cleanup
        workManager.startPeriodicCleanup()
    }

    // Global exception handler
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