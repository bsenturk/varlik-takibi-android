package com.xptlabs.varliktakibi.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.xptlabs.varliktakibi.MainActivity
import com.xptlabs.varliktakibi.R
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: FirebaseAnalyticsManager
) {

    companion object {
        private const val TAG = "AppNotificationManager"
        private const val CHANNEL_ID = "varlik_takibi_reminders"
        private const val CHANNEL_NAME = "Varlık Takibi Hatırlatmaları"
        private const val CHANNEL_DESCRIPTION = "Varlık takibi ve piyasa güncellemeleri"

        // Bildirim mesajları
        private val NOTIFICATION_MESSAGES = listOf(
            NotificationMessage(
                "Varlıklarınızı Kontrol Edin! 📊",
                "Portföyünüzde değişiklik olabilir. Güncel durumu kontrol etmek için uygulamayı açın."
            ),
            NotificationMessage(
                "Piyasa Güncellemesi! 📈",
                "Altın ve döviz kurlarında hareketlilik var. Varlıklarınızın durumunu inceleyin."
            ),
            NotificationMessage(
                "Kar/Zarar Takibi ⚡",
                "Portföyünüzün kar/zarar durumunu kontrol etmeyi unutmayın!"
            ),
            NotificationMessage(
                "Yatırım Fırsatları! 💰",
                "Güncel kurları kontrol ederek yeni yatırım fırsatlarını değerlendirin."
            ),
            NotificationMessage(
                "Varlık Portföyü Hatırlatması 🔔",
                "Uzun süredir uygulamayı açmadınız. Varlıklarınızın durumunu kontrol edin."
            )
        )
    }

    data class NotificationMessage(
        val title: String,
        val body: String
    )

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
        // Uygulama başlatıldığında test notification gönder (Android capability'sini tetiklemek için)
        sendInitialCapabilityNotification()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val systemNotificationManager = context.getSystemService(NotificationManager::class.java)
            systemNotificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    private fun sendInitialCapabilityNotification() {
        // Uygulama ilk kurulduğunda görünmez bir notification gönder
        // Bu Android'e uygulamanın notification capability'si olduğunu gösterir
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ için önce permission kontrolü
                if (areNotificationsEnabled()) {
                    sendCapabilityNotification()
                }
            } else {
                // Eski Android versiyonları için direkt gönder
                sendCapabilityNotification()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Capability notification not sent (normal): ${e.message}")
        }
    }

    private fun sendCapabilityNotification() {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Varlık Takibi")
                .setContentText("Uygulama hazır!")
                .setPriority(NotificationCompat.PRIORITY_MIN) // Minimum priority
                .setAutoCancel(true)
                .setTimeoutAfter(1000) // 1 saniye sonra otomatik sil
                .build()

            notificationManager.notify(999, notification)

            // Hemen iptal et
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                notificationManager.cancel(999)
            }, 1000)

            Log.d(TAG, "Capability notification sent and cancelled")
        } catch (e: Exception) {
            Log.d(TAG, "Capability notification failed: ${e.message}")
        }
    }

    fun scheduleNextNotification() {
        // 2-3 gün arası random delay (2-3 gün = 48-72 saat)
        val delayHours = Random.nextLong(48, 73) // 48-72 saat arası
        val delayMinutes = delayHours * 60

        // Random mesaj seç
        val messageIndex = Random.nextInt(NOTIFICATION_MESSAGES.size)
        val message = NOTIFICATION_MESSAGES[messageIndex]

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "title" to message.title,
                    "body" to message.body,
                    "messageIndex" to messageIndex
                )
            )
            .addTag("notification_reminder")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d(TAG, "Next notification scheduled in $delayHours hours with message: ${message.title}")

        // Analytics
        analyticsManager.logCustomEvent(
            eventName = "notification_scheduled",
            parameters = mapOf(
                "delay_hours" to delayHours,
                "message_index" to messageIndex,
                "message_title" to message.title
            )
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(title: String, body: String, messageIndex: Int = 0) {
        if (!areNotificationsEnabled()) {
            Log.d(TAG, "Notifications not enabled, skipping")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            putExtra("notification_message_index", messageIndex)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            messageIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        try {
            notificationManager.notify(messageIndex, notification)
            Log.d(TAG, "Notification shown: $title")

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "notification_shown",
                parameters = mapOf(
                    "title" to title,
                    "message_index" to messageIndex
                )
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun cancelAllScheduledNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag("notification_reminder")
        Log.d(TAG, "All scheduled notifications cancelled")

        // Analytics
        analyticsManager.logCustomEvent(
            eventName = "notifications_cancelled",
            parameters = emptyMap()
        )
    }

    fun getNotificationMessages(): List<NotificationMessage> {
        return NOTIFICATION_MESSAGES
    }

    // Test için manuel bildirim gönderme - Bu Android'e capability gösterir
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendTestNotification() {
        val testMessage = NOTIFICATION_MESSAGES.random()
        showNotification(testMessage.title, testMessage.body, 999)
    }

    // Force notification capability registration
    fun forceRegisterNotificationCapability() {
        try {
            createNotificationChannel()

            // Bir test notification gönder ve hemen iptal et
            if (areNotificationsEnabled()) {
                sendCapabilityNotification()
            }

            Log.d(TAG, "Notification capability forcefully registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register notification capability", e)
        }
    }
}