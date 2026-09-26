package com.xptlabs.varliktakibi.core.ext

import java.text.Normalizer
import java.util.Locale

/**
 * Arama eşleşmesi: büyük/küçük harf ve aksan duyarsız, Türkçe i/ı/İ/I eşit.
 * Fon adları tamamen büyük harf ve İ/Ş/Ö dolu; "is portfoy" "İŞ PORTFÖY"ü,
 * "bıt" Bitcoin'i bulmalı. i/ı çifti aksan katlamasıyla eşitlenmiyor (ı ayrı
 * bir kod noktası), o yüzden önce elle "i"ye indiriliyor.
 */
fun String.searchMatches(query: String): Boolean {
    val q = query.searchFolded()
    return q.isEmpty() || searchFolded().contains(q)
}

private val combiningMarks = Regex("\\p{Mn}+")

private fun String.searchFolded(): String {
    val dotless = trim().map { c ->
        when (c) {
            'I', 'ı', 'İ' -> 'i'
            else -> c
        }
    }.joinToString("")
    return combiningMarks.replace(Normalizer.normalize(dotless, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT)
}
