package com.xptlabs.varliktakibi.data.remote.parser

import android.util.Log
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetTrackerHtmlParser @Inject constructor() {

    companion object {
        private const val TAG = "HtmlParser"
    }

    fun parseGoldRates(html: String): List<RateEntity> {
        return try {
            Log.d(TAG, "Parsing gold rates - HTML length: ${html.length}")
            val doc: Document = Jsoup.parse(html)
            val goldRates = mutableListOf<RateEntity>()

            // iOS'taki gibi basit selector kullan
            val rows = doc.select("table tbody tr, table tr")
            Log.d(TAG, "Found ${rows.size} table rows")

            rows.forEach { row ->
                try {
                    val cells = row.select("td")

                    if (cells.size >= 4) {
                        val name = cells[0].text().trim()

                        Log.d(TAG, "Checking row: '$name'")

                        // iOS'taki gibi tam eşleşme kontrolü
                        if (isGoldName(name)) {
                            val buyPrice = cells[1].text().trim()
                            val sellPrice = cells[2].text().trim()
                            val change = if (cells.size > 3) cells[3].text().trim() else ""
                            val changePercent = if (cells.size > 4) cells[4].text().trim() else ""

                            Log.d(TAG, "Found gold: $name - Buy: $buyPrice, Sell: $sellPrice")

                            val goldRate = RateEntity(
                                id = getGoldId(name),
                                name = name,
                                type = "GOLD",
                                buyPrice = buyPrice.parsePrice(),
                                sellPrice = sellPrice.parsePrice(),
                                change = change.parseChangePercent(),
                                changePercent = changePercent.parseChangePercent(),
                                lastUpdated = Date(),
                                isChangePercentPositive = change.isChangePercentPositive()
                            )

                            // Sadece geçerli fiyatları ekle
                            if (goldRate.sellPrice > 0) {
                                goldRates.add(goldRate)
                                Log.d(TAG, "Added gold rate: ${goldRate.name} = ${goldRate.sellPrice} TL")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing gold row: ${e.message}")
                }
            }

            Log.d(TAG, "Total gold rates parsed: ${goldRates.size}")
            goldRates
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing gold rates", e)
            emptyList()
        }
    }

    fun parseCurrencyRates(html: String): List<RateEntity> {
        return try {
            Log.d(TAG, "Parsing currency rates - HTML length: ${html.length}")
            val doc: Document = Jsoup.parse(html)
            val currencyRates = mutableListOf<RateEntity>()

            // iOS'taki gibi basit selector kullan
            val rows = doc.select("table tbody tr, table tr")
            Log.d(TAG, "Found ${rows.size} table rows")

            rows.forEach { row ->
                try {
                    val cells = row.select("td")

                    if (cells.size >= 4) {
                        val name = cells[0].text().trim()

                        Log.d(TAG, "Checking row: '$name'")

                        // iOS'taki gibi kontrol
                        if (isCurrencyName(name)) {
                            val buyPrice = cells[1].text().trim()
                            val sellPrice = cells[2].text().trim()
                            val change = if (cells.size > 3) cells[3].text().trim() else ""
                            val changePercent = if (cells.size > 4) cells[4].text().trim() else ""

                            Log.d(TAG, "Found currency: $name - Buy: $buyPrice, Sell: $sellPrice")

                            val currencyRate = RateEntity(
                                id = getCurrencyId(name),
                                name = name,
                                type = "CURRENCY",
                                buyPrice = buyPrice.parsePrice(),
                                sellPrice = sellPrice.parsePrice(),
                                change = change.parseChangePercent(),
                                changePercent = changePercent.parseChangePercent(),
                                lastUpdated = Date(),
                                isChangePercentPositive = change.isChangePercentPositive()
                            )

                            // Sadece geçerli fiyatları ekle
                            if (currencyRate.sellPrice > 0) {
                                currencyRates.add(currencyRate)
                                Log.d(TAG, "Added currency rate: ${currencyRate.name} = ${currencyRate.sellPrice} TL")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing currency row: ${e.message}")
                }
            }

            Log.d(TAG, "Total currency rates parsed: ${currencyRates.size}")
            currencyRates
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing currency rates", e)
            emptyList()
        }
    }

    // iOS koduna göre helper methods
    private fun isGoldName(name: String): Boolean {
        val goldKeywords = listOf(
            "gram altın", "çeyrek altın", "yarım altın", "tam altın",
            "cumhuriyet altını", "ata altın", "beşli altın", "hamit altın", "reşat altın"
        )

        val lowercaseName = name.lowercase()
        // iOS gibi tam eşleşme kontrolü
        return goldKeywords.any { it == lowercaseName }
    }

    private fun isCurrencyName(name: String): Boolean {
        val currencyKeywords = listOf("USD", "EUR", "GBP")
        val uppercaseName = name.uppercase()

        // iOS gibi contains kontrolü
        return currencyKeywords.any { uppercaseName.contains(it) }
    }

    private fun getGoldId(name: String): String {
        val lowerName = name.lowercase()
        return when (lowerName) {
            "gram altın" -> "GOLD_GRAM"
            "çeyrek altın" -> "GOLD_QUARTER"
            "yarım altın" -> "GOLD_HALF"
            "tam altın" -> "GOLD_FULL"
            "cumhuriyet altını" -> "GOLD_REPUBLIC"
            "ata altın" -> "GOLD_ATA"
            "reşat altın" -> "GOLD_RESAT"
            "hamit altın" -> "GOLD_HAMIT"
            "beşli altın" -> "GOLD_BESLI"
            else -> "GOLD_UNKNOWN_${lowerName.replace(" ", "_")}"
        }
    }

    private fun getCurrencyId(name: String): String {
        val upperName = name.uppercase()
        return when {
            upperName.contains("USD") -> "USD"
            upperName.contains("EUR") -> "EUR"
            upperName.contains("GBP") -> "GBP"
            else -> "CURRENCY_UNKNOWN"
        }
    }

    // iOS'taki gibi basit parsing
    private fun String.parsePrice(): Double {
        return this.replace(".", "")      // Binlik ayracı
            .replace(",", ".")            // Ondalık ayracı
            .replace("₺", "")
            .replace("TL", "")
            .replace(" ", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun String.parseChange(): Double {
        return this.replace("₺", "")
            .replace("TL", "")
            .replace("+", "")
            .replace(" ", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun String.parseChangePercent(): Double {
        return this.replace("%", "")
            .replace("+", "")
            .replace("-", "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun String.isChangePercentPositive(): Boolean {
        return !this.contains("-")
    }
}