package com.xptlabs.varliktakibi.ui.common

/**
 * Sistem klavyesinden gelen sayı girişini temizler: yalnızca rakam ve tek bir
 * ondalık ayracı (virgül), ondalık hane sınırı. Klavye yerele göre "." ya da ","
 * verebiliyor, yapıştırılan metin de serbest — ikisi de aynı biçime iniyor.
 */
object DecimalInput {

    fun sanitize(raw: String, maxDecimals: Int): String {
        val out = StringBuilder()
        var decimals = -1 // ayraçtan sonraki hane sayısı; -1 = ayraç yok
        for (c in raw) {
            when {
                c.isDigit() -> {
                    if (decimals >= maxDecimals) continue
                    // Baştaki gereksiz sıfırı yut: "05" → "5", ama "0,5" kalır.
                    if (out.toString() == "0" && decimals < 0) out.clear()
                    out.append(c)
                    if (decimals >= 0) decimals++
                }
                (c == ',' || c == '.') && decimals < 0 && maxDecimals > 0 -> {
                    if (out.isEmpty()) out.append('0')
                    out.append(',')
                    decimals = 0
                }
            }
        }
        return out.toString()
    }
}
