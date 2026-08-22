package com.xptlabs.varliktakibi.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.xptlabs.varliktakibi.R
import com.xptlabs.varliktakibi.data.remote.PushTokenService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihazın FCM token'ını Supabase `device_tokens` tablosuna kaydeder, böylece
 * `market-alert` işi bu cihaza da push atabilir.
 *
 * Uygulama anonim (Supabase Auth yok); cihazlar iOS'taki `identifierForVendor`
 * karşılığı olan ANDROID_ID ile tanımlanıyor.
 */
@Singleton
class PushRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenService: PushTokenService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
        )
    }

    /** Sistem ayarlarından bildirimler açık mı (kanal bazlı kapatma dahil). */
    fun notificationsEnabled(): Boolean {
        val systemAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return systemAllowed
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return systemAllowed && granted
    }

    /**
     * Güncel token'ı çekip kaydeder. İzin durumu her değiştiğinde ve uygulama
     * ön plana geldiğinde çağrılır; token değişmemişse backend'de upsert no-op.
     */
    fun sync() {
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                tokenService.register(deviceId(), token, notificationsEnabled())
            }.onFailure { Log.w(TAG, "Push token sync başarısız: ${it.message}") }
        }
    }

    /** Firebase yeni token verdiğinde ([AppFirebaseMessagingService] çağırır). */
    fun onNewToken(token: String) {
        scope.launch {
            runCatching { tokenService.register(deviceId(), token, notificationsEnabled()) }
                .onFailure { Log.w(TAG, "Yeni token kaydedilemedi: ${it.message}") }
        }
    }

    /** Kullanıcı bildirimleri kapattığında bayrağı backend'de de indir. */
    fun setEnabled(enabled: Boolean) {
        scope.launch {
            runCatching { tokenService.setEnabled(deviceId(), enabled) }
                .onFailure { Log.w(TAG, "Bildirim bayrağı güncellenemedi: ${it.message}") }
        }
    }

    @Suppress("HardwareIds") // Anonim uygulamada cihaz kimliği için tek seçenek.
    private fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-${context.packageName}"

    private companion object {
        const val TAG = "PushRegistrar"
    }
}
