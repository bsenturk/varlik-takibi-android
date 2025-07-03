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

    // Open app settings
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Check if we should show rationale for notification
    fun shouldShowNotificationRationale(): Boolean {
        return isNotificationPermissionRequired()
    }
}