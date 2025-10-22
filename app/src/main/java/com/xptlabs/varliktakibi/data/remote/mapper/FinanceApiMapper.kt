package com.xptlabs.varliktakibi.data.remote.mapper

import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.data.remote.dto.CommonRateDto
import com.xptlabs.varliktakibi.data.remote.dto.FinanceApiResponse
import com.xptlabs.varliktakibi.domain.models.AssetType
import java.util.Date
import java.util.UUID

/**
 * Maps Finance API response to RateEntity list
 */
object FinanceApiMapper {

    fun mapToRateEntities(response: FinanceApiResponse): List<RateEntity> {
        val rates = mutableListOf<RateEntity>()
        val dto = response.rates

        // Map currencies (only if not null)
        dto.usd?.let { rates.add(it.toRateEntity("USD", "CURRENCY")) }
        dto.eur?.let { rates.add(it.toRateEntity("EUR", "CURRENCY")) }
        dto.gbp?.let { rates.add(it.toRateEntity("GBP", "CURRENCY")) }

        // Map gold products (only if not null)
        dto.gra?.let { rates.add(it.toRateEntity("GRA", "GOLD")) }
        dto.ceyrek?.let { rates.add(it.toRateEntity("CEYREK", "GOLD")) }
        dto.yarim?.let { rates.add(it.toRateEntity("YARIM", "GOLD")) }
        dto.tam?.let { rates.add(it.toRateEntity("TAM", "GOLD")) }
        dto.cumhuriyet?.let { rates.add(it.toRateEntity("CUMHURIYET", "GOLD")) }
        dto.ata?.let { rates.add(it.toRateEntity("ATA", "GOLD")) }
        dto.resat?.let { rates.add(it.toRateEntity("RESAT", "GOLD")) }
        dto.hamit?.let { rates.add(it.toRateEntity("HAMIT", "GOLD")) }
        dto.besli?.let { rates.add(it.toRateEntity("BESLI", "GOLD")) }
        dto.gremse?.let { rates.add(it.toRateEntity("GREMSE", "GOLD")) }
        dto.ayar14?.let { rates.add(it.toRateEntity("14AYAR", "GOLD")) }
        dto.ayar18?.let { rates.add(it.toRateEntity("18AYAR", "GOLD")) }
        dto.ikiBucuk?.let { rates.add(it.toRateEntity("IKIbucuk", "GOLD")) }
        dto.ayar22?.let { rates.add(it.toRateEntity("22AYAR", "GOLD")) }

        // Map silver (only if not null)
        dto.gumus?.let { rates.add(it.toRateEntity("GUMUS", "SILVER")) }

        return rates
    }

    private fun CommonRateDto.toRateEntity(code: String, type: String): RateEntity {
        val changeValue = change ?: 0.0
        val sellingValue = selling ?: 0.0
        val changePercent = calculateChangePercent()

        return RateEntity(
            id = code,  // Use code as ID so MarketDataManager can find it
            name = name ?: code,  // Fallback to code if name is null
            type = type,
            buyPrice = buying ?: 0.0,
            sellPrice = sellingValue,
            change = changeValue,
            changePercent = changePercent,
            lastUpdated = Date(),
            isChangePercentPositive = changeValue >= 0
        )
    }

    private fun CommonRateDto.calculateChangePercent(): Double {
        val sellingValue = selling ?: 0.0
        val changeValue = change ?: 0.0

        return if (sellingValue > 0) {
            (changeValue / sellingValue) * 100
        } else {
            0.0
        }
    }

    /**
     * Get rate for specific asset type
     */
    fun getRateForAssetType(response: FinanceApiResponse, assetType: AssetType): CommonRateDto? {
        return when (assetType.apiKey) {
            "USD" -> response.rates.usd
            "EUR" -> response.rates.eur
            "GBP" -> response.rates.gbp
            "GRA" -> response.rates.gra
            "CEYREK" -> response.rates.ceyrek
            "YARIM" -> response.rates.yarim
            "TAM" -> response.rates.tam
            "CUMHURIYET" -> response.rates.cumhuriyet
            "ATA" -> response.rates.ata
            "RESAT" -> response.rates.resat
            "HAMIT" -> response.rates.hamit
            "BESLI" -> response.rates.besli
            "GREMSE" -> response.rates.gremse
            "14AYAR" -> response.rates.ayar14
            "18AYAR" -> response.rates.ayar18
            "IKIbucuk" -> response.rates.ikiBucuk
            "22AYAR" -> response.rates.ayar22
            "GUMUS" -> response.rates.gumus
            else -> null
        }
    }
}
