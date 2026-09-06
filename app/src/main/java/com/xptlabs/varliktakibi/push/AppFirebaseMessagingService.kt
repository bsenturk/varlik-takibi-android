package com.xptlabs.varliktakibi.push

import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Bildirim gövdesini FCM'in kendisi çiziyor (backend düz `notification` payload
 * gönderiyor, bkz. supabase/functions/send-push-notification). Burada tek iş
 * yeni token'ı backend'e bildirmek.
 */
@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registrar.onNewToken(token)
    }
}
