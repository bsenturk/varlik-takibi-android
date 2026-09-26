package com.xptlabs.varliktakibi.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Paywall'ın nereden açıldığı — üst başlığı ve analitiği belirler. */
enum class PaywallContext(val key: String, val headline: String, val subtitle: String) {
    GENERAL(
        "general",
        "Portföyünün\ntamamını gör.",
        "Sınırsız portföy, TEFAS fonları ve reklamsız bir deneyim."
    ),
    ONBOARDING(
        "onboarding",
        "Portföyünün\ntamamını gör.",
        "Sınırsız portföy, TEFAS fonları ve reklamsız bir deneyim."
    ),
    FUND(
        "fund",
        "Fonlarını da\nburada takip et.",
        "TEFAS fonlarını ara, ekle ve portföyünle birlikte değerlendir."
    ),
    PORTFOLIO_LIMIT(
        "portfolio_limit",
        "Her hedefe\nayrı portföy.",
        "Emeklilik, tatil, acil durum… Sınırsız portföy aç."
    ),
    ADS(
        "ads",
        "Reklamlar olmadan\ndaha rahat.",
        "Pro ile tüm reklamlar kalkar, uygulama akıcı kalır."
    );

    companion object {
        fun fromKey(key: String?): PaywallContext =
            entries.firstOrNull { it.key == key } ?: GENERAL
    }
}

data class PlanOption(
    val pkg: Package,
    val label: String,
    val periodSuffix: String,
    val price: String,
    val hasTrial: Boolean
)

data class PaywallUiState(
    val plans: List<PlanOption> = emptyList(),
    val selectedPlan: PlanOption? = null,
    val isPurchasing: Boolean = false,
    /** RevenueCat yapılandırılmadıysa (Play ürünleri henüz bağlı değil). */
    val isUnavailable: Boolean = false,
    val message: String? = null,
    val purchaseSucceeded: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val purchaseManager: PurchaseManager,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    private val selectedPackageId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val succeeded = MutableStateFlow(false)

    val uiState: StateFlow<PaywallUiState> = combine(
        purchaseManager.currentOffering,
        purchaseManager.purchaseInProgress,
        selectedPackageId,
        message,
        succeeded
    ) { offering, purchasing, selectedId, msg, done ->
        val plans = offering?.availablePackages.orEmpty().mapNotNull(::toPlanOption)
        PaywallUiState(
            plans = plans,
            selectedPlan = plans.firstOrNull { it.pkg.identifier == selectedId }
                // Varsayılan seçim yıllık: hem en iyi değer hem iOS'la aynı.
                ?: plans.firstOrNull { it.pkg.packageType == PackageType.ANNUAL }
                ?: plans.firstOrNull(),
            isPurchasing = purchasing,
            isUnavailable = !purchaseManager.isConfigured,
            message = msg,
            purchaseSucceeded = done
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    init {
        viewModelScope.launch { purchaseManager.loadOfferings() }
    }

    /** Paywall'ın ekranda kaldığı süreyi ölçmek için — refleks kapatmayı ayırır. */
    private var shownAt = 0L

    fun onShown(context: PaywallContext) {
        shownAt = System.currentTimeMillis()
        analytics.logPaywallShown(context.key)
    }

    fun onDismissed(context: PaywallContext) {
        val seconds = if (shownAt == 0L) 0
        else ((System.currentTimeMillis() - shownAt) / 1000L).toInt()
        analytics.logPaywallDismissed(context.key, seconds)
    }

    fun selectPlan(plan: PlanOption) {
        selectedPackageId.value = plan.pkg.identifier
        analytics.logPaywallPlanSelected(plan.label)
    }

    fun purchase(activity: Activity) {
        val plan = uiState.value.selectedPlan ?: return
        analytics.logPaywallCtaTapped(plan.label, plan.hasTrial)
        viewModelScope.launch {
            when (val outcome = purchaseManager.purchase(activity, plan.pkg)) {
                is PurchaseManager.PurchaseOutcome.Success -> {
                    analytics.logSubscriptionPurchased(plan.label, plan.hasTrial)
                    succeeded.value = true
                }
                is PurchaseManager.PurchaseOutcome.Cancelled ->
                    analytics.logSubscriptionPurchaseFailed(plan.label, "cancelled", null)
                is PurchaseManager.PurchaseOutcome.Failed ->
                    analytics.logSubscriptionPurchaseFailed(plan.label, "error", outcome.errorCode)
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            val restored = purchaseManager.restore()
            if (restored) {
                analytics.logSubscriptionRestored()
                succeeded.value = true
            } else {
                message.value = "Geri yüklenecek aktif abonelik bulunamadı."
            }
        }
    }

    fun clearMessage() { message.value = null }

    private fun toPlanOption(pkg: Package): PlanOption? {
        val (label, suffix) = when (pkg.packageType) {
            PackageType.ANNUAL -> "Yıllık" to "yıl"
            PackageType.MONTHLY -> "Aylık" to "ay"
            PackageType.WEEKLY -> "Haftalık" to "hafta"
            // Diğer paket tipleri (lifetime, custom) bu paywall'da gösterilmiyor.
            else -> return null
        }
        return PlanOption(
            pkg = pkg,
            label = label,
            periodSuffix = suffix,
            price = pkg.product.price.formatted,
            // Play'de ücretsiz deneme, abonelik seçeneğinin ücretsiz fiyat
            // aşaması olarak modelleniyor. Kullanıcı denemeyi daha önce
            // kullandıysa Play bu aşamayı hiç döndürmüyor — yani "Ücretsiz Dene"
            // yazıp anında ücret almış olmuyoruz.
            hasTrial = pkg.product.defaultOption?.freePhase != null
        )
    }
}
