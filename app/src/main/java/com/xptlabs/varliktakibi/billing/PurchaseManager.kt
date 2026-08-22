package com.xptlabs.varliktakibi.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Varlık Pro" aboneliği için RevenueCat sarmalayıcısı. iOS `PurchaseManager`
 * portu; entitlement kimliği iki platformda ortak (aynı RevenueCat projesi).
 *
 * API anahtarı boşsa (Play ürünleri henüz bağlanmadıysa) SDK hiç kurulmaz ve
 * uygulama ücretsiz modda sorunsuz çalışır — paywall "şu an kullanılamıyor"
 * der, çökmez.
 */
@Singleton
class PurchaseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _currentOffering = MutableStateFlow<Offering?>(null)
    val currentOffering: StateFlow<Offering?> = _currentOffering.asStateFlow()

    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress.asStateFlow()

    /** SDK kuruldu mu — anahtar yoksa false kalır. */
    val isConfigured: Boolean get() = configured

    private var configured = false

    fun configure() {
        val apiKey = BuildConfig.REVENUECAT_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "RevenueCat API key yok; abonelik devre dışı.")
            // Kalıcı bayrağı yine de oku ki daha önce Pro olmuş kullanıcı
            // anahtar geçici olarak boşken reklam yemesin.
            scope.launch { prefs.isPro.collect { _isPro.value = it } }
            return
        }

        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        configured = true

        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> applyEntitlement(info) }

        scope.launch {
            refreshCustomerInfo()
            loadOfferings()
        }
    }

    suspend fun loadOfferings() {
        if (!configured) return
        runCatching { Purchases.sharedInstance.awaitOfferings() }
            .onSuccess { _currentOffering.value = it.current }
            .onFailure { Log.e(TAG, "Offerings yüklenemedi: ${it.message}") }
    }

    suspend fun refreshCustomerInfo() {
        if (!configured) return
        runCatching { Purchases.sharedInstance.awaitCustomerInfo() }
            .onSuccess { applyEntitlement(it) }
            .onFailure { Log.e(TAG, "CustomerInfo alınamadı: ${it.message}") }
    }

    /** @return abonelik aktifleştiyse true. Kullanıcı iptal ederse false. */
    suspend fun purchase(activity: Activity, pkg: Package): Boolean {
        if (!configured) return false
        _purchaseInProgress.value = true
        return try {
            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            applyEntitlement(result.customerInfo)
            isSubscribed(result.customerInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Satın alma başarısız: ${e.message}")
            false
        } finally {
            _purchaseInProgress.value = false
        }
    }

    suspend fun restore(): Boolean {
        if (!configured) return false
        _purchaseInProgress.value = true
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            applyEntitlement(info)
            isSubscribed(info)
        } catch (e: Exception) {
            Log.e(TAG, "Geri yükleme başarısız: ${e.message}")
            false
        } finally {
            _purchaseInProgress.value = false
        }
    }

    private fun isSubscribed(info: CustomerInfo): Boolean =
        info.entitlements[ENTITLEMENT_ID]?.isActive == true ||
            info.entitlements.active.isNotEmpty()

    private fun applyEntitlement(info: CustomerInfo) {
        val pro = isSubscribed(info)
        if (_isPro.value != pro) Log.d(TAG, "Pro entitlement → $pro")
        _isPro.value = pro
        scope.launch { prefs.setPro(pro) }
    }

    companion object {
        private const val TAG = "PurchaseManager"

        /** RevenueCat dashboard'daki entitlement kimliği — iOS ile aynı. */
        const val ENTITLEMENT_ID = "pro"
    }
}
