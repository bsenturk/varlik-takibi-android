package com.xptlabs.varliktakibi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.xptlabs.varliktakibi.managers.AssetTrackerWorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VarlikTakibiApplication(override val workManagerConfiguration: Configuration) : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: AssetTrackerWorkManager

    override fun onCreate() {
        super.onCreate()

        // Start periodic background work
        setupBackgroundWork()
    }

    private fun setupBackgroundWork() {
        // Start periodic sync every 15 minutes
        workManager.startPeriodicSync()

        // Start daily cleanup
        workManager.startPeriodicCleanup()
    }
}