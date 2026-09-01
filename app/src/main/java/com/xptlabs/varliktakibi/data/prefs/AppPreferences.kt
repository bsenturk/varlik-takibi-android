package com.xptlabs.varliktakibi.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.xptlabs.varliktakibi.core.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class DarkModePreference(val displayName: String) {
    SYSTEM("Sistem"), LIGHT("Açık"), DARK("Koyu");

    companion object {
        fun fromName(value: String?): DarkModePreference =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * iOS `UserDefaultsManager` karşılığı. Kalıcı ama önemsiz tercihler; portföy
 * verisi Room'da.
 */
@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("has_seen_onboarding")
        val DARK_MODE = stringPreferencesKey("dark_mode_preference")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val SELECTED_PORTFOLIO = stringPreferencesKey("selected_portfolio_id")
        val MASKED_PORTFOLIOS = stringSetPreferencesKey("masked_portfolio_ids")

        // Onboarding devri: tanıtım sayfalarından sonra varlık ekleme akışı
        // kendiliğinden açılsın, kullanıcı gerçek varlığını ekledikten sonra
        // paywall bir kez görünsün.
        val PENDING_FIRST_ASSET_ADD = booleanPreferencesKey("pending_first_asset_add")
        val PENDING_ONBOARDING_PAYWALL = booleanPreferencesKey("pending_onboarding_paywall")

        val AD_OPPORTUNITY_COUNT = stringPreferencesKey("ad_opportunity_count")
        val ASSET_ADD_COUNT = stringPreferencesKey("asset_add_count")
        val REVIEW_ASKED = booleanPreferencesKey("review_asked")
        val LAST_SNAPSHOT_DAY = stringPreferencesKey("last_snapshot_day")
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    suspend fun setOnboardingCompleted() = edit { it[Keys.ONBOARDING_DONE] = true }

    val darkMode: Flow<DarkModePreference> =
        dataStore.data.map { DarkModePreference.fromName(it[Keys.DARK_MODE]) }
    suspend fun setDarkMode(value: DarkModePreference) = edit { it[Keys.DARK_MODE] = value.name }

    val isPro: Flow<Boolean> = dataStore.data.map { it[Keys.IS_PRO] ?: false }
    suspend fun setPro(value: Boolean) = edit { it[Keys.IS_PRO] = value }

    val selectedCurrency: Flow<Currency> =
        dataStore.data.map { Currency.fromCode(it[Keys.SELECTED_CURRENCY]) }
    suspend fun setSelectedCurrency(value: Currency) =
        edit { it[Keys.SELECTED_CURRENCY] = value.code }

    val selectedPortfolioId: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_PORTFOLIO] }
    suspend fun setSelectedPortfolioId(id: String) = edit { it[Keys.SELECTED_PORTFOLIO] = id }

    /** "Varlıkları gizle" göz ikonu — portföy bazlı, id kümesi olarak. */
    val maskedPortfolioIds: Flow<Set<String>> =
        dataStore.data.map { it[Keys.MASKED_PORTFOLIOS] ?: emptySet() }

    suspend fun togglePortfolioMask(id: String) = edit { prefs ->
        val current = prefs[Keys.MASKED_PORTFOLIOS] ?: emptySet()
        prefs[Keys.MASKED_PORTFOLIOS] = if (id in current) current - id else current + id
    }

    val pendingFirstAssetAdd: Flow<Boolean> =
        dataStore.data.map { it[Keys.PENDING_FIRST_ASSET_ADD] ?: false }
    suspend fun setPendingFirstAssetAdd(value: Boolean) =
        edit { it[Keys.PENDING_FIRST_ASSET_ADD] = value }

    val pendingOnboardingPaywall: Flow<Boolean> =
        dataStore.data.map { it[Keys.PENDING_ONBOARDING_PAYWALL] ?: false }
    suspend fun setPendingOnboardingPaywall(value: Boolean) =
        edit { it[Keys.PENDING_ONBOARDING_PAYWALL] = value }

    /** Reklam fırsatı sayacı — her 3.'sünde interstitial yerine paywall. */
    suspend fun nextAdOpportunity(): Int {
        var result = 0
        dataStore.edit { prefs ->
            result = (prefs[Keys.AD_OPPORTUNITY_COUNT]?.toIntOrNull() ?: 0) + 1
            prefs[Keys.AD_OPPORTUNITY_COUNT] = result.toString()
        }
        return result
    }

    /** Kaçıncı varlık eklendi — puan istemi bu sayaca bakıyor. */
    suspend fun nextAssetAddCount(): Int {
        var result = 0
        dataStore.edit { prefs ->
            result = (prefs[Keys.ASSET_ADD_COUNT]?.toIntOrNull() ?: 0) + 1
            prefs[Keys.ASSET_ADD_COUNT] = result.toString()
        }
        return result
    }

    val reviewAsked: Flow<Boolean> = dataStore.data.map { it[Keys.REVIEW_ASKED] ?: false }
    suspend fun setReviewAsked() = edit { it[Keys.REVIEW_ASKED] = true }

    /** Günlük anlık görüntü işinin en son hangi günü yazdığı. */
    val lastSnapshotDay: Flow<Long> =
        dataStore.data.map { it[Keys.LAST_SNAPSHOT_DAY]?.toLongOrNull() ?: 0L }
    suspend fun setLastSnapshotDay(day: Long) =
        edit { it[Keys.LAST_SNAPSHOT_DAY] = day.toString() }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }
}
