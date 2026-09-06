package com.xptlabs.varliktakibi.core.format

import com.xptlabs.varliktakibi.core.model.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Tüm sayı gösterimi tek yerden: binlik ayracı ".", ondalık ",".
 *
 * DecimalFormat thread-safe değil; her çağrıda yenisini kurmak yerine
 * ThreadLocal'da tutuluyor (liste kaydırmada satır başına çağrılıyor).
 */
object TrFormat {

    private val TR = Locale.forLanguageTag("tr-TR")
    private val symbols = DecimalFormatSymbols(TR)

    private val formatters: ThreadLocal<MutableMap<String, DecimalFormat>> =
        ThreadLocal.withInitial { mutableMapOf() }

    private fun formatter(pattern: String): DecimalFormat =
        // withInitial garanti ettiği için get() hiç null dönmez; tip bildirimi
        // Kotlin'in platform tipini nullable görmesini engelliyor.
        formatters.get()!!.getOrPut(pattern) { DecimalFormat(pattern, symbols) }

    /** "12.345,67" */
    fun decimal(value: Double, decimals: Int = 2): String {
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        return formatter(pattern).format(value)
    }

    /** Tutar + para birimi sembolü. TL sonda, diğerleri başta (iOS ile aynı). */
    fun money(value: Double, currency: Currency = Currency.TRY): String = when (currency) {
        Currency.TRY -> "${decimal(value)} ₺"
        else -> "${currency.symbol}${decimal(value)}"
    }

    /**
     * Miktar: tam sayıysa ondalık gösterme, değilse en fazla 4 hane
     * (0,00021 BTC gibi girdiler yuvarlanıp sıfırlanmasın).
     */
    fun amount(value: Double): String =
        if (value % 1.0 == 0.0) decimal(value, 0) else formatter("#,##0.####").format(value)

    /**
     * İşaretli yüzde. Sıfır olmayan ama %0,01'in altındaki değer "%0,00" diye
     * gösterilirse kullanıcı kâr yokmuş sanıyor — iOS'taki gibi "<0,01" yazılıyor.
     */
    fun percent(value: Double): String {
        val sign = if (value >= 0) "+" else "-"
        val magnitude = abs(value)
        if (magnitude > 0 && magnitude < 0.01) return "$sign<0,01%"
        return "$sign${decimal(magnitude)}%"
    }

    /** Pasta dilimi etiketi: küçük ama var olan dilim "%0" değil "<%1". */
    fun slicePercent(value: Double): String =
        if (value > 0 && value < 1) "<%1" else "%${decimal(value, 0)}"

    /** Değer gizliyken (göz ikonu kapalı) gösterilen maske. */
    const val MASK = "••••••"
}
