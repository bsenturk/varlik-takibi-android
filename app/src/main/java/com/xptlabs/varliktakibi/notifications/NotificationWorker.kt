package com.xptlabs.varliktakibi.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: AppNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // Input data'dan bildirim bilgilerini al
            val title = inputData.getString("title") ?: "Varlık Takibi"
            val body = inputData.getString("body") ?: "Varlıklarınızı kontrol edin!"
            val messageIndex = inputData.getInt("messageIndex", 0)

            Log.d(TAG, "Showing scheduled notification: $title")

            // Bildirimi göster
            notificationManager.showNotification(title, body, messageIndex)

            // Bir sonraki bildirimi planla
            notificationManager.scheduleNextNotification()

            Log.d(TAG, "Notification work completed successfully")
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Notification work failed", exception)
            Result.failure()
        }
    }
}