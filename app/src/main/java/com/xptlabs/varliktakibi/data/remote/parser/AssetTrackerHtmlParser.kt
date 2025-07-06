package com.xptlabs.varliktakibi.data.remote.parser

import com.xptlabs.varliktakibi.data.remote.dto.RateDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetTrackerHtmlParser @Inject constructor() {

    fun parseGoldRates(html: String): List<RateDto> {
        return try {
            val doc: Document = Jsoup.parse(html)
            val rates = mutableListOf<RateDto>()

            // Farklı selector'ları dene
            val possibleSelectors = listOf(
                "table tbody tr",
                ".data-table tbody tr",
                ".market-data tr",
                "tr"
            )

            var rows: org.jsoup.select.Elements? = null
            for (selector in possibleSelectors) {
                rows = doc.select(selector)
                if (rows.size > 0) break
            }

            rows?.forEach { row ->
                try {
                    val cells = row.select("td")
                    if (cells.size >= 3) {
                        val name = cells[0].text().trim()

                        if (isGoldName(name)) {
                            val buyPrice = cells.getOrNull(1)?.text()?.trim() ?: ""
                            val sellPrice = cells.getOrNull(2)?.text()?.trim() ?: ""
                            val change = cells.getOrNull(3)?.text()?.trim() ?: ""
                            val changePercent = cells.getOrNull(4)?.text()?.trim() ?: ""

                            rates.add(
                                RateDto(
                                    name = name,
                                    code = null,
                                    buyPrice = buyPrice,
                                    sellPrice = sellPrice,
                                    change = change,
                                    changePercent = changePercent
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Bu satırı atla, devam et
                }
            }

            rates
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseCurrencyRates(html: String): List<RateDto> {
        return try {
            val doc: Document = Jsoup.parse(html)
            val rates = mutableListOf<RateDto>()

            // Farklı selector'ları dene
            val possibleSelectors = listOf(
                "table tbody tr",
                ".data-table tbody tr",
                ".market-data tr",
                "tr"
            )

            var rows: org.jsoup.select.Elements? = null
            for (selector in possibleSelectors) {
                rows = doc.select(selector)
                if (rows.size > 0) break
            }

            rows?.forEach { row ->
                try {
                    val cells = row.select("td")
                    if (cells.size >= 3) {
                        val name = cells[0].text().trim()

                        if (isCurrencyName(name)) {
                            val buyPrice = cells.getOrNull(1)?.text()?.trim() ?: ""
                            val sellPrice = cells.getOrNull(2)?.text()?.trim() ?: ""
                            val change = cells.getOrNull(3)?.text()?.trim() ?: ""
                            val changePercent = cells.getOrNull(4)?.text()?.trim() ?: ""

                            rates.add(
                                RateDto(
                                    name = name,
                                    code = extractCurrencyCode(name),
                                    buyPrice = buyPrice,
                                    sellPrice = sellPrice,
                                    change = change,
                                    changePercent = changePercent
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Bu satırı atla, devam et
                }
            }

            rates
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isGoldName(name: String): Boolean {
        val goldKeywords = listOf(
            "gram altın", "çeyrek altın", "yarım altın", "tam altın",
            "cumhuriyet altını", "ata altın", "beşli altın",
            "hamit altın", "reşat altın", "gram", "çeyrek", "yarım"
        )
        val lowerName = name.lowercase()
        return goldKeywords.any { lowerName.contains(it) }
    }

    private fun isCurrencyName(name: String): Boolean {
        val currencyKeywords = listOf("USD", "EUR", "GBP", "DOLAR", "EURO", "STERLİN", "POUND")
        val upperName = name.uppercase()
        return currencyKeywords.any { upperName.contains(it) }
    }

    private fun extractCurrencyCode(name: String): String {
        val upperName = name.uppercase()
        return when {
            upperName.contains("USD") || upperName.contains("DOLAR") -> "USD"
            upperName.contains("EUR") || upperName.contains("EURO") -> "EUR"
            upperName.contains("GBP") || upperName.contains("STERLİN") || upperName.contains("POUND") -> "GBP"
            else -> "TRY"
        }
    }
}