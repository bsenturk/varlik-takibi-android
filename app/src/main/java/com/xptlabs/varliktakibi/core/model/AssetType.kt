package com.xptlabs.varliktakibi.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * iOS `MyGolds/Models/AssetType.swift` portu.
 *
 * `supabaseSymbol` değerleri backend'in `assets_prices` tablosuna yazdığı
 * sembollerle **birebir** aynı olmak zorunda; kaynak liste
 * `supabase/functions/fetch-gold-fx/index.ts`. Bir harf sapması o varlığın
 * fiyatının hiç bulunamamasına yol açar (AssetTypeTest bunu doğruluyor).
 */

/** "Genel" portföyün gruplama seviyesi; varlık ekleme akışındaki ilk adım. */
enum class AssetCategory(
    val displayName: String,
    val icon: ImageVector,
    val tintHex: String
) {
    GOLD("Altın", Icons.Filled.Hive, "#FFB300"),
    SILVER("Gümüş", Icons.Filled.Workspaces, "#9E9E9E"),
    CURRENCY("Döviz", Icons.Filled.Payments, "#34C759"),
    CRYPTO("Kripto", Icons.Filled.CurrencyBitcoin, "#F7931A"),
    BIST("Borsa İstanbul", Icons.AutoMirrored.Filled.ShowChart, "#E63946"),
    US_STOCK("ABD Borsası", Icons.Filled.AccountBalance, "#2A9D8F"),
    FUND("Fon", Icons.Filled.PieChart, "#5856D6");

    /**
     * Dinamik kategorilerin enstrümanları sabit değil, canlı `assets_prices`
     * kataloğundan geliyor (kripto / hisse / fon).
     */
    val isDynamic: Boolean get() = backendAssetType != null

    /** Varlık Pro gerektiren kategoriler. */
    val isPremium: Boolean get() = this == FUND

    /** Dinamik kategoriler için `assets_prices.asset_type` değeri. */
    val backendAssetType: String?
        get() = when (this) {
            CRYPTO -> "crypto"
            BIST -> "bist"
            US_STOCK -> "us_stock"
            FUND -> "fund"
            GOLD, SILVER, CURRENCY -> null
        }

    /** Dinamik kategoride tutulan varlıkları etiketleyen jenerik tür. */
    val dynamicAssetType: AssetType?
        get() = when (this) {
            CRYPTO -> AssetType.CRYPTO
            BIST -> AssetType.BIST_STOCK
            US_STOCK -> AssetType.US_STOCK
            FUND -> AssetType.FUND
            GOLD, SILVER, CURRENCY -> null
        }

    /** Bu kategoriye ait sabit türler, görüntüleme sırasıyla. Dinamiklerde boş. */
    val assetTypes: List<AssetType>
        get() = if (isDynamic) emptyList()
        else AssetType.entries.filter { it.category == this }
}

/** Döviz kurları saf veri: her biri için ayrı `when` dalı yerine tek tablo. */
data class FxInfo(
    /** `assets_prices` sembolü, aynı zamanda gösterilen birim. */
    val symbol: String,
    val displayName: String,
    /** Ülke bayrağı emojisi — ikon yerine geçer. */
    val flag: String,
    val tintHex: String
)

enum class AssetType(val id: String) {
    GOLD("gold"),
    GOLD_QUARTER("gold_quarter"),
    GOLD_HALF("gold_half"),
    GOLD_FULL("gold_full"),
    GOLD_REPUBLIC("gold_republic"),
    GOLD_ATA("gold_ata"),
    GOLD_RESAT("gold_resat"),
    GOLD_HAMIT("gold_hamit"),
    GOLD_FIVE("gold_five"),
    GOLD_GREMSE("gold_gremse"),
    GOLD_FOURTEEN("gold_fourteen"),
    GOLD_EIGHTEEN("gold_eighteen"),
    GOLD_TWO_AND_HALF("gold_twoandhalf"),
    GOLD_TWENTYTWO_BRACELET("gold_twentytwo_bracelet"),
    SILVER("silver"),
    TL("tl"),
    USD("usd"),
    EUR("eur"),
    GBP("gbp"),
    CHF("chf"),
    SAR("sar"),
    CAD("cad"),
    RUB("rub"),
    AED("aed"),
    AUD("aud"),
    DKK("dkk"),
    SEK("sek"),
    NOK("nok"),
    JPY("jpy"),
    KWD("kwd"),

    // Dinamik piyasa enstrümanları. Hangi enstrüman olduğu `Asset.symbol`'den
    // belli; bu jenerik türler yalnızca kategori/ikon bilgisi taşır.
    CRYPTO("crypto"),
    BIST_STOCK("bist_stock"),
    US_STOCK("us_stock"),
    FUND("fund");

    val fx: FxInfo? get() = FX[this]

    /** Sembolle sürülen jenerik piyasa türü mü (kripto / hisse / fon)? */
    val isDynamic: Boolean
        get() = this == CRYPTO || this == BIST_STOCK || this == US_STOCK || this == FUND

    val displayName: String
        get() = fx?.displayName ?: when (this) {
            GOLD -> "Gram Altın"
            GOLD_QUARTER -> "Çeyrek Altın"
            GOLD_HALF -> "Yarım Altın"
            GOLD_FULL -> "Tam Altın"
            GOLD_REPUBLIC -> "Cumhuriyet Altını"
            GOLD_ATA -> "Ata Altın"
            GOLD_RESAT -> "Reşat Altın"
            GOLD_HAMIT -> "Hamit Altın"
            GOLD_FIVE -> "Beşli Altın"
            GOLD_GREMSE -> "Gremse Altın"
            GOLD_FOURTEEN -> "14 Ayar Altın"
            GOLD_EIGHTEEN -> "18 Ayar Altın"
            GOLD_TWO_AND_HALF -> "İki Buçuk Altın"
            GOLD_TWENTYTWO_BRACELET -> "22 Ayar Bilezik"
            SILVER -> "Gram Gümüş"
            CRYPTO -> "Kripto Para"
            BIST_STOCK -> "BIST Hisse"
            US_STOCK -> "ABD Hisse"
            FUND -> "Yatırım Fonu"
            else -> id
        }

    // iOS'ta 14/18 ayar ve 22 ayar bilezik de "adet" — gramla satılsalar da.
    // Birebir parite için aynen korunuyor; değişirse iki platformun aynı varlığı
    // farklı birimle göstermesi gerekir.
    val unit: String
        get() = fx?.symbol ?: when (this) {
            GOLD, SILVER -> "gram"
            BIST_STOCK, US_STOCK -> "lot"
            else -> "adet"
        }

    /** FX ise bayrak emojisi, değilse null (o zaman [icon] kullanılır). */
    val flag: String? get() = fx?.flag

    val icon: ImageVector
        get() = when (this) {
            SILVER -> Icons.Filled.Workspaces
            CRYPTO -> Icons.Filled.CurrencyBitcoin
            BIST_STOCK -> Icons.AutoMirrored.Filled.ShowChart
            US_STOCK -> Icons.Filled.AccountBalance
            FUND -> Icons.Filled.PieChart
            else -> if (fx != null) Icons.Filled.Payments else Icons.Filled.Hive
        }

    val tintHex: String
        get() = fx?.tintHex ?: when (this) {
            SILVER -> "#9E9E9E"
            CRYPTO -> "#F7931A"
            BIST_STOCK -> "#E63946"
            US_STOCK -> "#2A9D8F"
            FUND -> "#5856D6"
            else -> "#FFB300"
        }

    /**
     * `assets_prices` tablosunda bu türü bulmak için kullanılan sembol.
     * Dinamik türlerin sabit sembolü yok — arama anahtarı `Asset.symbol`.
     */
    val supabaseSymbol: String
        get() = fx?.symbol ?: when (this) {
            GOLD -> "GRAM_ALTIN"
            GOLD_QUARTER -> "CEYREK_ALTIN"
            GOLD_HALF -> "YARIM_ALTIN"
            GOLD_FULL -> "TAM_ALTIN"
            GOLD_REPUBLIC -> "CUMHURIYET_ALTIN"
            GOLD_ATA -> "ATA_ALTIN"
            GOLD_RESAT -> "RESAT_ALTIN"
            GOLD_HAMIT -> "HAMIT_ALTIN"
            GOLD_FIVE -> "BESLI_ALTIN"
            GOLD_GREMSE -> "GREMSE_ALTIN"
            GOLD_FOURTEEN -> "14_AYAR_ALTIN"
            GOLD_EIGHTEEN -> "18_AYAR_ALTIN"
            GOLD_TWO_AND_HALF -> "IKIBUCUK_ALTIN"
            GOLD_TWENTYTWO_BRACELET -> "22_AYAR_BILEZIK"
            SILVER -> "GRAM_GUMUS"
            CRYPTO, BIST_STOCK, US_STOCK, FUND -> ""
            else -> id.uppercase()
        }

    /** "Genel" portföyde hangi üst kategoriye toplandığı. */
    val category: AssetCategory
        get() = when {
            fx != null -> AssetCategory.CURRENCY
            this == SILVER -> AssetCategory.SILVER
            this == CRYPTO -> AssetCategory.CRYPTO
            this == BIST_STOCK -> AssetCategory.BIST
            this == US_STOCK -> AssetCategory.US_STOCK
            this == FUND -> AssetCategory.FUND
            else -> AssetCategory.GOLD
        }

    companion object {
        /**
         * Desteklenen dövizler. Yeni bir kur eklemek = buraya bir satır +
         * backend'deki FX listesi (supabase/functions/fetch-gold-fx).
         */
        val FX: Map<AssetType, FxInfo> = mapOf(
            TL to FxInfo("TRY", "Türk Lirası", "🇹🇷", "#FF3B30"),
            USD to FxInfo("USD", "Dolar", "🇺🇸", "#34C759"),
            EUR to FxInfo("EUR", "Euro", "🇪🇺", "#0A84FF"),
            GBP to FxInfo("GBP", "Sterlin", "🇬🇧", "#AF52DE"),
            CHF to FxInfo("CHF", "İsviçre Frangı", "🇨🇭", "#FF453A"),
            SAR to FxInfo("SAR", "Suudi Riyali", "🇸🇦", "#30D158"),
            CAD to FxInfo("CAD", "Kanada Doları", "🇨🇦", "#FF6B6B"),
            RUB to FxInfo("RUB", "Rus Rublesi", "🇷🇺", "#5E5CE6"),
            AED to FxInfo("AED", "BAE Dirhemi", "🇦🇪", "#2A9D8F"),
            AUD to FxInfo("AUD", "Avustralya Doları", "🇦🇺", "#FF9F0A"),
            DKK to FxInfo("DKK", "Danimarka Kronu", "🇩🇰", "#C9184A"),
            SEK to FxInfo("SEK", "İsveç Kronu", "🇸🇪", "#0077B6"),
            NOK to FxInfo("NOK", "Norveç Kronu", "🇳🇴", "#457B9D"),
            JPY to FxInfo("JPY", "Japon Yeni", "🇯🇵", "#E63946"),
            KWD to FxInfo("KWD", "Kuveyt Dinarı", "🇰🇼", "#6A994E")
        )

        fun fromId(id: String): AssetType? = entries.firstOrNull { it.id == id }
    }
}
