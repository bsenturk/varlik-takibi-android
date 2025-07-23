package com.xptlabs.varliktakibi.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

object PermissionHelper {

    // Notification permission (Android 13+)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    // Check if notification permission is needed
    fun isNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    // Open app settings - İyileştirilmiş versiyon
    fun openAppSettings(context: Context) {
        try {
            // Önce notification settings'i dene
            openNotificationSettings(context)
        } catch (e: Exception) {
            // Fallback: Genel app settings
            openGeneralAppSettings(context)
        }
    }

    // Notification settings'e direkt git
    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8+ için notification channel settings
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Eski versiyonlar için genel app settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            // En son fallback
            openGeneralAppSettings(context)
        }
    }

    // Genel app settings
    private fun openGeneralAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Son çare: genel settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // Check if we should show rationale for notification
    fun shouldShowNotificationRationale(): Boolean {
        return isNotificationPermissionRequired()
    }

    // Notification izni kontrol et
    fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Eski versiyonlarda notification permission yok
        }
    }
}