package com.xptlabs.varliktakibi.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihazın FCM token'ını Supabase'e yazar.
 *
 * Yazma doğrudan tablo upsert'ü ile değil SECURITY DEFINER RPC'leriyle yapılıyor:
 * anon rolüne `device_tokens` üzerinde SELECT/INSERT/UPDATE verilseydi public
 * anon key'i olan herkes tüm cihazların token'ını okuyabilirdi.
 */
@Singleton
class PushTokenService @Inject constructor(
    private val client: SupabaseClient
) {

    suspend fun register(deviceId: String, fcmToken: String, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            client.postgrest.rpc(
                function = "register_device_token",
                parameters = RegisterParams(
                    p_device_id = deviceId,
                    p_fcm_token = fcmToken,
                    p_platform = PLATFORM,
                    p_notifications_enabled = enabled
                )
            )
            Unit
        }

    suspend fun setEnabled(deviceId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        client.postgrest.rpc(
            function = "set_device_token_enabled",
            parameters = EnabledParams(p_device_id = deviceId, p_enabled = enabled)
        )
        Unit
    }

    @Serializable
    private data class RegisterParams(
        val p_device_id: String,
        val p_fcm_token: String,
        val p_platform: String,
        val p_notifications_enabled: Boolean
    )

    @Serializable
    private data class EnabledParams(
        val p_device_id: String,
        val p_enabled: Boolean
    )

    private companion object {
        const val PLATFORM = "android"
    }
}
