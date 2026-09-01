package com.xptlabs.varliktakibi.review

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play In-App Review akışı: kullanıcı uygulamadan çıkmadan puan verebiliyor.
 *
 * Ne zaman soracağımız Play'in kotasından bağımsız olarak burada da
 * sınırlanıyor. Play günde/haftada kaç kez gösterileceğine kendi karar veriyor
 * ve istem hiç açılmayabilir — bu yüzden "gösterildi mi" diye bir bilgi yok,
 * `launchReview` sessizce döner. Sorabildiğimiz an kıymetli, o yüzden erken
 * harcamıyoruz: kullanıcı [ASK_AFTER_ASSET] varlığını ekleyene kadar bekliyoruz
 * ki uygulamayı gerçekten kullanmış biri puanlasın.
 */
@Singleton
class ReviewPrompter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
    private val analytics: FirebaseAnalyticsManager
) {

    /**
     * Varlık ekleme akışının kapanışında çağrılır.
     *
     * @param otherOverlayShowing reklam ya da paywall açılıyorsa true; sayaç
     *   yine işler ama üst üste ikinci bir tam ekran çıkarmayız.
     */
    suspend fun onAssetAdded(activity: Activity, otherOverlayShowing: Boolean) {
        // Sayaç her eklemede ilerler; istemi gösterip göstermeyeceğimiz ayrı karar.
        val count = prefs.nextAssetAddCount()
        if (!shouldAsk(count, prefs.reviewAsked.first(), otherOverlayShowing)) return

        // ponytail: başarısız olsa da bir daha sormuyoruz. Play Store'u olmayan
        // cihazda her eklemede yeniden denemek boşuna; ikinci bir şans istenirse
        // bayrağı yalnızca başarıda yaz.
        prefs.setReviewAsked()
        analytics.logReviewPromptRequested(count)

        runCatching {
            val manager = ReviewManagerFactory.create(context)
            manager.launchReview(activity, manager.requestReview())
        }.onFailure { Log.w(TAG, "Puan istemi açılamadı: ${it.message}") }
    }

    private companion object {
        const val TAG = "ReviewPrompter"
    }
}

/** Kaçıncı varlık eklendiğinde soralım. */
internal const val ASK_AFTER_ASSET = 3

/** Saf karar — test edilebilsin diye [ReviewPrompter] dışında. */
internal fun shouldAsk(count: Int, alreadyAsked: Boolean, otherOverlayShowing: Boolean): Boolean =
    !otherOverlayShowing && !alreadyAsked && count >= ASK_AFTER_ASSET
