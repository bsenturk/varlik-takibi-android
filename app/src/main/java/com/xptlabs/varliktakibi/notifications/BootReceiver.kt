package com.xptlabs.varliktakibi.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: AppNotificationManager

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Uygulama kurulduğunda/güncellendiğinde notification capability'sini tetikle
                try {
                    // Notification channel'ı oluştur
                    notificationManager.createNotificationChannel()

                    // Scheduled notification'ları yeniden planla
                    notificationManager.scheduleNextNotification()

                    Log.d(TAG, "Notification system reinitialized after boot/update")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reinitialize notification system", e)
                }
            }
        }
    }
}