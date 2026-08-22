package com.xptlabs.varliktakibi.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `assets_prices` tablosuna salt okunur erişim. Tüm fiyatlar (hisse, kripto,
 * altın, döviz, fon) backend Edge Function'ları tarafından üretiliyor — uygulama
 * hiçbir üçüncü parti fiyat API'sine gitmiyor.
 *
 * iOS `MyGolds/Supabase/Services/MarketDataService.swift` karşılığı.
 */
@Singleton
class MarketDataService @Inject constructor(
    private val client: SupabaseClient
) {

    /** `assets_prices` tablosundaki tüm güncel fiyat satırları. */
    suspend fun fetchLivePrices(): List<AssetPrice> = withContext(Dispatchers.IO) {
        client.from(TABLE).select().decodeList<AssetPrice>()
    }

    /**
     * Verilen sembollerin [from, to] aralığındaki kapanış fiyatları. Time Machine
     * eksik günleri bununla yeniden kuruyor.
     */
    suspend fun fetchHistoricalPrices(
        symbols: List<String>,
        from: Instant,
        to: Instant
    ): List<AssetPrice> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyList()
        client.from(TABLE).select {
            filter {
                isIn("symbol", symbols)
                gte("updated_at", from.toString())
                lte("updated_at", to.toString())
            }
            order("updated_at", Order.ASCENDING)
        }.decodeList()
    }

    /**
     * TEFAS'ta fon arar. Backend fonları canlı çeker, `assets_prices`'a upsert
     * eder ve eşleşen satırları döner — yani seçilen fon anında fiyatlanabilir.
     */
    suspend fun searchFunds(query: String): List<AssetPrice> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val response = client.functions.invoke(
            function = "search-tefas",
            body = FundSearchRequest(q = trimmed)
        )
        // Gövdeyi elle çözüyoruz: yanıtın ContentNegotiation'dan geçmesine
        // güvenmek yerine tek bir bilinen Json yapılandırması kullanılıyor.
        json.decodeFromString<FundSearchResponse>(response.bodyAsText()).data
    }

    @Serializable
    private data class FundSearchRequest(val q: String)

    @Serializable
    private data class FundSearchResponse(val data: List<AssetPrice> = emptyList())

    private companion object {
        const val TABLE = "assets_prices"
        val json = Json { ignoreUnknownKeys = true }
    }
}
