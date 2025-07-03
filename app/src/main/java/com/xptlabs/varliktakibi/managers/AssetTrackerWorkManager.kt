package com.xptlabs.varliktakibi.managers

import android.content.Context
import androidx.work.*
import com.xptlabs.varliktakibi.workers.AssetTrackerCleanupWorker
import com.xptlabs.varliktakibi.workers.AssetTrackerSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetTrackerWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<AssetTrackerSyncWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AssetTrackerSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    fun startPeriodicCleanup() {
        val cleanupWork = PeriodicWorkRequestBuilder<AssetTrackerCleanupWorker>(
            1, TimeUnit.DAYS // Daily cleanup
        )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("cleanup")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AssetTrackerCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWork
        )
    }

    fun startOneTimeSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSync = OneTimeWorkRequestBuilder<AssetTrackerSyncWorker>()
            .setConstraints(constraints)
            .addTag("one_time_sync")
            .build()

        workManager.enqueue(oneTimeSync)
    }

    fun stopAllWork() {
        workManager.cancelAllWorkByTag("sync")
        workManager.cancelAllWorkByTag("cleanup")
    }

    fun getWorkInfo(workName: String) = workManager.getWorkInfosForUniqueWork(workName)
}