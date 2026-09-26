package com.xptlabs.varliktakibi.market

import android.util.Log
import com.xptlabs.varliktakibi.core.ext.parseTimestampOrNull
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.data.remote.AssetPrice
import com.xptlabs.varliktakibi.data.remote.ChartRange
import com.xptlabs.varliktakibi.data.remote.PriceSeries
import com.xptlabs.varliktakibi.data.remote.MarketDataService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gösterime hazır bir piyasa enstrümanı. Fiyat her zaman TL cinsinden —
 * USD fiyatlı satırlar canlı USD kuruyla çevrilir, böylece portföy tek
 * para biriminde toplanabilir.
 */
data class Instrument(
    val symbol: String,
    val name: String,
    val priceTry: Double,
    val changePercent: Double?,
    val category: AssetCategory,
    /** Sabit tür (altın/döviz) ise dolu; dinamik enstrümanlarda jenerik tür. */
    val type: AssetType,
    val unit: String,
    val flag: String? = null,
    val logoUrl: String? = null
)

/**
 * Canlı fiyatların tek kaynağı. iOS `MarketDataManager.swift` portu.
 *
 * Yenileme uygulama ön plandayken 60 saniyede bir; arka planda döngü durur
 * (günlük anlık görüntüyü WorkManager yazıyor, bkz. SnapshotWorker).
 */
@Singleton
class MarketDataStore @Inject constructor(
    private val service: MarketDataService
) {

    private val _prices = MutableStateFlow<List<AssetPrice>>(emptyList())
    val prices: StateFlow<List<AssetPrice>> = _prices.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastUpdate = MutableStateFlow<Instant?>(null)
    val lastUpdate: StateFlow<Instant?> = _lastUpdate.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Eşzamanlı iki yenileme birbirinin sonucunu ezmesin. */
    private val refreshLock = Mutex()

    suspend fun refresh() = refreshLock.withLock {
        _isRefreshing.value = true
        try {
            val fetched = service.fetchLivePrices()
            _prices.value = fetched
            _lastUpdate.value = Instant.now()
            _error.value = null
            Log.d(TAG, "Refreshed ${fetched.size} price rows")
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            // Eski fiyatlar korunur; kullanıcıya "son güncelleme" zamanı gösterilir.
            _error.value = "Piyasa verileri alınamadı. Bağlantınızı kontrol edin."
        } finally {
            _isRefreshing.value = false
        }
    }

    fun clearError() { _error.value = null }

    // ── TL fiyatlandırma ─────────────────────────────────────────────────────

    /** 1 USD kaç TL — USD fiyatlı enstrümanları çevirmek için. */
    private val usdToTry: Double?
        get() = _prices.value.firstOrNull { it.symbol == "USD" && it.currency == "TRY" }?.price

    /**
     * Sembolün TL cinsinden güncel fiyatı. Önce doğal TRY satırı; yoksa USD
     * satırı canlı kurla çevrilir. Bulunamazsa **null** — sabit bir varsayılan
     * fiyat döndürmek bayat veriyi gerçekmiş gibi gösterirdi.
     */
    fun tryPrice(symbol: String): Double? {
        if (symbol == "TRY") return 1.0
        val rows = _prices.value.filter { it.symbol == symbol }
        if (rows.isEmpty()) return null

        rows.firstOrNull { it.currency == "TRY" }?.let { return it.price }
        val usdRow = rows.firstOrNull { it.currency == "USD" }
        val rate = usdToTry
        if (usdRow != null && rate != null) return usdRow.price * rate
        return null
    }

    /** Sembolün gün içi değişim yüzdesi (backend'in hesapladığı). */
    fun changePercent(symbol: String): Double? =
        _prices.value.firstOrNull { it.symbol == symbol && it.currency == "TRY" }?.changePercent
            ?: _prices.value.firstOrNull { it.symbol == symbol }?.changePercent

    /**
     * Enstrümanın kendi logosu; aynı sembolün bütün satırları aynı logoyu
     * taşıyor. null normal (altın, döviz, fon, logosu bulunamayan hisse).
     */
    fun logoUrl(symbol: String): String? =
        _prices.value.firstOrNull { it.symbol == symbol && it.logoUrl != null }?.logoUrl

    fun lastUpdateOf(symbol: String): Instant? =
        _prices.value.firstOrNull { it.symbol == symbol }?.updatedAt?.parseTimestampOrNull()

    // ── Kategori katalogları ─────────────────────────────────────────────────

    /** Sabit kategoriler (altın/gümüş/döviz) için tür listesinden üretilir. */
    private fun staticInstruments(category: AssetCategory): List<Instrument> =
        category.assetTypes.mapNotNull { type ->
            // TL'nin kur listesinde işi yok (1 TRY = 1 TRY), ama varlık olarak eklenebilir.
            val price = tryPrice(type.supabaseSymbol) ?: return@mapNotNull null
            Instrument(
                symbol = type.supabaseSymbol,
                name = type.displayName,
                priceTry = price,
                changePercent = changePercent(type.supabaseSymbol),
                category = category,
                type = type,
                unit = type.unit,
                flag = type.flag
            )
        }

    /** Dinamik kategoriler (kripto/hisse/fon) için canlı katalogdan üretilir. */
    private fun dynamicInstruments(category: AssetCategory): List<Instrument> {
        val backendType = category.backendAssetType ?: return emptyList()
        val genericType = category.dynamicAssetType ?: return emptyList()

        return _prices.value
            .filter { it.assetType == backendType }
            .groupBy { it.symbol }
            .mapNotNull { (symbol, rows) ->
                val price = tryPrice(symbol) ?: return@mapNotNull null
                Instrument(
                    symbol = symbol,
                    name = displayName(symbol, rows.firstOrNull()?.name, backendType),
                    priceTry = price,
                    changePercent = (rows.firstOrNull { it.currency == "TRY" } ?: rows.first())
                        .changePercent,
                    category = category,
                    type = genericType,
                    unit = genericType.unit,
                    logoUrl = rows.firstNotNullOfOrNull { it.logoUrl }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun instruments(category: AssetCategory): List<Instrument> = when {
        category.isDynamic -> dynamicInstruments(category)
        category.isManual -> manualInstruments(category)
        else -> staticInstruments(category)
    }

    /**
     * Elle girilen türler (ev, araba, BES…): fiyatı yok, değeri kullanıcı giriyor.
     * Sembol yer tutucu — kayıtta varlığa özel `MANUAL-<id>` ile değiştiriliyor.
     */
    private fun manualInstruments(category: AssetCategory): List<Instrument> =
        category.assetTypes.map { type ->
            Instrument(
                symbol = type.id,
                name = type.displayName,
                priceTry = 0.0,
                changePercent = null,
                category = category,
                type = type,
                unit = type.unit
            )
        }

    /**
     * Varlığın güncel TL birim fiyatı: elle girilende kullanıcının son girdiği
     * değer (piyasada karşılığı yok), diğerlerinde canlı fiyat.
     */
    fun priceOf(asset: AssetEntity): Double? =
        if (asset.assetType.isManual) asset.currentPrice else tryPrice(asset.symbol)

    /**
     * Okunabilir enstrüman adı: BIST'te ".IS" son eki atılır, kriptoda
     * CoinGecko id'si baş harfleri büyütülür, ABD hissesinde sembolün kendisi.
     */
    private fun displayName(symbol: String, rawName: String?, backendType: String): String =
        when (backendType) {
            "bist" -> symbol.removeSuffix(".IS")
            "us_stock", "us_etf" -> symbol
            // `name` CoinGecko id'si ("avalanche-2", "binancecoin") — backend
            // grafik/logo için onu id olarak kullanıyor, değiştirilemez.
            "crypto" -> rawName?.takeIf { it.isNotBlank() }?.let(::cryptoDisplayName) ?: symbol
            else -> rawName ?: symbol
        }

    /** Enstrüman detayındaki fiyat grafiği için; hata yukarı fırlar. */
    suspend fun priceSeries(symbol: String, range: ChartRange): PriceSeries =
        service.fetchPriceSeries(symbol, range)

    // ── TEFAS araması ────────────────────────────────────────────────────────

    /**
     * Backend'de fon arar ve sonuçları canlı fiyat kümesine katar, böylece
     * seçilen fon anında fiyatlanabilir. Hata yutulur — yerel katalog çalışmaya
     * devam eder.
     */
    suspend fun searchFunds(query: String): List<Instrument> {
        val results = runCatching { service.searchFunds(query) }
            .onFailure { Log.w(TAG, "Fund search failed: ${it.message}") }
            .getOrDefault(emptyList())

        if (results.isNotEmpty()) {
            _prices.update { current ->
                val merged = LinkedHashMap<String, AssetPrice>(current.size + results.size)
                current.forEach { merged["${it.symbol}_${it.currency}"] = it }
                results.forEach { merged["${it.symbol}_${it.currency}"] = it }
                merged.values.toList()
            }
        }
        return instruments(AssetCategory.FUND)
    }

    // ── Otomatik yenileme ────────────────────────────────────────────────────

    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    /** Uygulama ön plana geldiğinde çağrılır. Zaten çalışıyorsa no-op. */
    fun startAutoRefresh(scope: CoroutineScope) {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = scope.launch {
            while (true) {
                refresh()
                kotlinx.coroutines.delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    /** Arka plana geçerken çağrılır — boşuna ağ trafiği ve pil harcamasın. */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    companion object {
        private const val TAG = "MarketDataStore"

        /** Genel kuralın ("shiba-inu" → "Shiba Inu") yanlış sonuç verdiği id'ler. */
        private val CRYPTO_NAMES = mapOf(
            "binancecoin" to "BNB",
            "ripple" to "XRP",
            "avalanche-2" to "Avalanche",
            "the-open-network" to "Toncoin",
            "usd-coin" to "USD Coin",
            "render-token" to "Render",
            "hedera-hashgraph" to "Hedera",
            "injective-protocol" to "Injective",
            "polygon-ecosystem-token" to "Polygon",
            "worldcoin-wld" to "Worldcoin",
            "jupiter-exchange-solana" to "Jupiter",
            "sei-network" to "Sei",
            "fetch-ai" to "Fetch.ai",
            "ondo-finance" to "Ondo",
            "vechain" to "VeChain",
            "near" to "NEAR",
            "tron" to "TRON"
        )

        /** CoinGecko id'sinden okunur ad: önce eşleme, yoksa tire → boşluk + baş harf. */
        fun cryptoDisplayName(id: String): String =
            CRYPTO_NAMES[id] ?: id.replace('-', ' ').split(' ').joinToString(" ") { w ->
                w.replaceFirstChar { it.titlecase(java.util.Locale.ROOT) }
            }

        private const val REFRESH_INTERVAL_MS = 60_000L
    }
}
