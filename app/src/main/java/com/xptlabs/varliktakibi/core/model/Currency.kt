package com.xptlabs.varliktakibi.core.model

import java.util.Locale

/**
 * Toplam değerin gösterileceği para birimi (yalnızca sunum; hesap hep TL'de).
 *
 * Liste, uygulamanın döviz olarak zaten desteklediği kurların tamamı: ad ve
 * bayrak [AssetType.FX] ile aynı kaynaktan okunuyor ki iki liste birbirinden
 * kaymasın.
 */
enum class Currency(val code: String) {
    TRY("TRY"), USD("USD"), EUR("EUR"), GBP("GBP"), CHF("CHF"), SAR("SAR"), CAD("CAD"),
    RUB("RUB"), AED("AED"), AUD("AUD"), DKK("DKK"), SEK("SEK"), NOK("NOK"), JPY("JPY"),
    KWD("KWD");

    private val fx: FxInfo? get() = AssetType.FX.values.firstOrNull { it.symbol == code }

    val flag: String get() = fx?.flag ?: "🏳️"

    /** "Dolar", "İsviçre Frangı" … */
    val displayName: String get() = fx?.displayName ?: code

    /**
     * "₺", "$", "CHF" … Türkçe yerelin bu kod için kullandığı işaret. Elle
     * tutulan tablo yerine platformdan: bir kısmında ($, €, ¥) gerçek işaret,
     * kalanında ISO kodunun kendisi doğru olan.
     */
    val symbol: String
        get() = runCatching { java.util.Currency.getInstance(code).getSymbol(TR) }.getOrDefault(code)

    companion object {
        private val TR = Locale.forLanguageTag("tr-TR")

        fun fromCode(code: String?): Currency =
            entries.firstOrNull { it.code == code } ?: TRY
    }
}
