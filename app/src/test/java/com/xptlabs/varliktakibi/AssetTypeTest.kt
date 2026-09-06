package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.core.model.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sembol eşlemesi backend'le birebir tutmalı. Aşağıdaki liste
 * `supabase/functions/fetch-gold-fx/index.ts` içindeki GOLD + FX dizilerinden
 * kopyalandı; oradaki bir değişiklik burada kırmızıya dönmeli, aksi halde o
 * varlığın fiyatı sessizce hiç bulunamaz.
 */
class AssetTypeTest {

    private val backendSymbols = setOf(
        // GOLD
        "GRAM_ALTIN", "CEYREK_ALTIN", "YARIM_ALTIN", "TAM_ALTIN", "CUMHURIYET_ALTIN",
        "ATA_ALTIN", "RESAT_ALTIN", "HAMIT_ALTIN", "BESLI_ALTIN", "GREMSE_ALTIN",
        "14_AYAR_ALTIN", "18_AYAR_ALTIN", "IKIBUCUK_ALTIN", "22_AYAR_BILEZIK",
        "GRAM_GUMUS",
        // FX
        "USD", "EUR", "GBP", "CHF", "SAR", "CAD", "RUB", "AED", "AUD", "DKK",
        "SEK", "NOK", "JPY", "KWD"
    )

    @Test
    fun `sabit turlerin sembolleri backend listesinde var`() {
        val missing = AssetType.entries
            .filterNot { it.isDynamic }
            // TRY'nin assets_prices'ta satırı yok; 1 TRY = 1 TRY olarak
            // MarketDataStore içinde özel olarak ele alınıyor.
            .filterNot { it == AssetType.TL }
            .filterNot { it.supabaseSymbol in backendSymbols }

        assertTrue("Backend'de karşılığı olmayan türler: $missing", missing.isEmpty())
    }

    @Test
    fun `dinamik turlerin sabit sembolu yok`() {
        AssetType.entries.filter { it.isDynamic }.forEach { type ->
            assertEquals("${type.name} sabit sembol taşımamalı", "", type.supabaseSymbol)
        }
    }

    @Test
    fun `her sembol tek bir ture ait`() {
        val symbols = AssetType.entries.filterNot { it.isDynamic }.map { it.supabaseSymbol }
        assertEquals("Sembol çakışması var", symbols.size, symbols.toSet().size)
    }

    @Test
    fun `gumus kendi kategorisinde ama altin gibi maden`() {
        // Eski Android sürümündeki hata: gümüş "GOLD" tipiyle filtrelendiği için
        // hiç bulunamıyor ve sabit bir varsayılan fiyata düşüyordu.
        assertEquals(AssetCategory.SILVER, AssetType.SILVER.category)
        assertEquals("GRAM_GUMUS", AssetType.SILVER.supabaseSymbol)
    }

    @Test
    fun `doviz turleri para birimi kategorisinde ve bayrak tasiyor`() {
        AssetType.FX.keys.forEach { type ->
            assertEquals(AssetCategory.CURRENCY, type.category)
            assertTrue("${type.name} bayraksız", !type.flag.isNullOrBlank())
        }
    }

    @Test
    fun `dinamik kategoriler backend tipiyle eslesir`() {
        assertEquals("crypto", AssetCategory.CRYPTO.backendAssetType)
        assertEquals("bist", AssetCategory.BIST.backendAssetType)
        assertEquals("us_stock", AssetCategory.US_STOCK.backendAssetType)
        assertEquals("fund", AssetCategory.FUND.backendAssetType)
        assertEquals(null, AssetCategory.GOLD.backendAssetType)
    }
}
