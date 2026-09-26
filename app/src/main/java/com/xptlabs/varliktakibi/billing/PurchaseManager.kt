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
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val prefs: AppPreferences,
    private val analytics: FirebaseAnalyticsManager
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

    /**
     * Debug build'de Pro durumunu sabitler; null ise gerçek abonelik geçerli.
     * RevenueCat'in `updatedCustomerInfoListener`'ı ve her açılıştaki
     * `refreshCustomerInfo()` aksi hâlde zorlamayı saniyeler içinde eziyordu.
     */
    private var debugProOverride: Boolean? = null

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

        scope.launch {
            // Sıra önemli: RevenueCat kendi önbelleğinden anında bir
            // CustomerInfo yollayabiliyor. Dinleyiciyi ya da yenilemeyi önce
            // bağlarsak diskteki değerler okunmadan `_isPro` yazılıyor ve debug
            // zorlaması saniyelerce etkisiz kalıyordu.
            if (BuildConfig.DEBUG) {
                debugProOverride = prefs.debugProOverride.first()
            }
            // RevenueCat cevap verene kadar kalıcı bayrak geçerli: aksi hâlde
            // Pro kullanıcı her açılışta bir an ücretsiz sayılıp reklam
            // yüklüyordu.
            _isPro.value = debugProOverride ?: prefs.isPro.first()

            Purchases.sharedInstance.updatedCustomerInfoListener =
                UpdatedCustomerInfoListener { info -> applyEntitlement(info) }

            refreshCustomerInfo()
            loadOfferings()
        }

        // Zorlama çalışırken de değiştirilebiliyor; sonraki değişimler buradan.
        if (BuildConfig.DEBUG) {
            scope.launch {
                prefs.debugProOverride.collect { override ->
                    debugProOverride = override
                    if (override != null) _isPro.value = override
                }
            }
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

    /**
     * Satın alma sonucu. Düz `Boolean` döndürüldüğünde kullanıcı iptali ile
     * gerçek store hatası aynı sayıya düşüyor ve huninin son adımı okunamıyordu.
     */
    sealed interface PurchaseOutcome {
        data object Success : PurchaseOutcome
        /** Kullanıcı Play satın alma sayfasını kapattı. */
        data object Cancelled : PurchaseOutcome
        data class Failed(val errorCode: Int?) : PurchaseOutcome
    }

    suspend fun purchase(activity: Activity, pkg: Package): PurchaseOutcome {
        if (!configured) return PurchaseOutcome.Failed(null)
        _purchaseInProgress.value = true
        return try {
            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            applyEntitlement(result.customerInfo)
            if (isSubscribed(result.customerInfo)) PurchaseOutcome.Success
            else PurchaseOutcome.Failed(null)
        } catch (e: PurchasesTransactionException) {
            Log.e(TAG, "Satın alma başarısız: ${e.message}")
            if (e.userCancelled) PurchaseOutcome.Cancelled
            else PurchaseOutcome.Failed(e.code.code)
        } catch (e: Exception) {
            Log.e(TAG, "Satın alma başarısız: ${e.message}")
            PurchaseOutcome.Failed(null)
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

    /** Yalnızca debug: `null` zorlamayı kaldırır, gerçek aboneliğe döner. */
    fun setDebugProOverride(value: Boolean?) {
        if (!BuildConfig.DEBUG) return
        scope.launch {
            prefs.setDebugProOverride(value)
            // Zorlama kalkınca gerçek durum yeniden okunsun.
            if (value == null) refreshCustomerInfo()
        }
    }

    private fun applyEntitlement(info: CustomerInfo) {
        debugProOverride?.let {
            Log.d(TAG, "Debug Pro zorlaması açık ($it); entitlement yok sayıldı")
            return
        }
        val pro = isSubscribed(info)
        if (_isPro.value != pro) Log.d(TAG, "Pro entitlement → $pro")
        _isPro.value = pro
        // Firebase'deki her raporun ücretsiz/Pro kırılımı buna bağlı.
        analytics.setProUser(pro)
        scope.launch { prefs.setPro(pro) }
    }

    companion object {
        private const val TAG = "PurchaseManager"

        /** RevenueCat dashboard'daki entitlement kimliği — iOS ile aynı. */
        const val ENTITLEMENT_ID = "pro"
    }
}
