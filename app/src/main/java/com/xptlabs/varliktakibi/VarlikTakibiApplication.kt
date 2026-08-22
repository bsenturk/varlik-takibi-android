package com.xptlabs.varliktakibi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.history.SnapshotScheduler
import com.xptlabs.varliktakibi.push.PushRegistrar
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VarlikTakibiApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var analytics: FirebaseAnalyticsManager
    @Inject lateinit var adMobManager: AdMobManager
    @Inject lateinit var purchaseManager: PurchaseManager
    @Inject lateinit var repository: PortfolioRepository
    @Inject lateinit var snapshotScheduler: SnapshotScheduler
    @Inject lateinit var pushRegistrar: PushRegistrar

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE_NAME)
        }
        analytics.setCollectionEnabled(!BuildConfig.DEBUG)

        purchaseManager.configure()
        adMobManager.registerActivityLifecycleCallbacks(this)
        adMobManager.preloadAppOpenAd()
        pushRegistrar.createNotificationChannel()

        appScope.launch {
            // Varlık ekleme akışı bir portföy olmadan çalışamaz; ilk ekran
            // çizilmeden önce garanti altına alınıyor.
            repository.ensureDefaults()
        }
        snapshotScheduler.schedule()
    }
}
