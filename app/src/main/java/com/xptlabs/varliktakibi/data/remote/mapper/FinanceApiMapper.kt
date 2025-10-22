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

        // Map currencies
        rates.add(dto.usd.toRateEntity("USD", "CURRENCY"))
        rates.add(dto.eur.toRateEntity("EUR", "CURRENCY"))
        rates.add(dto.gbp.toRateEntity("GBP", "CURRENCY"))

        // Map gold products
        rates.add(dto.gra.toRateEntity("GRA", "GOLD"))
        rates.add(dto.ceyrek.toRateEntity("CEYREK", "GOLD"))
        rates.add(dto.yarim.toRateEntity("YARIM", "GOLD"))
        rates.add(dto.tam.toRateEntity("TAM", "GOLD"))
        rates.add(dto.cumhuriyet.toRateEntity("CUMHURIYET", "GOLD"))
        rates.add(dto.ata.toRateEntity("ATA", "GOLD"))
        rates.add(dto.resat.toRateEntity("RESAT", "GOLD"))
        rates.add(dto.hamit.toRateEntity("HAMIT", "GOLD"))
        rates.add(dto.besli.toRateEntity("BESLI", "GOLD"))
        rates.add(dto.gremse.toRateEntity("GREMSE", "GOLD"))
        rates.add(dto.ayar14.toRateEntity("14AYAR", "GOLD"))
        rates.add(dto.ayar18.toRateEntity("18AYAR", "GOLD"))
        rates.add(dto.ikiBucuk.toRateEntity("IKIbucuk", "GOLD"))
        rates.add(dto.ayar22.toRateEntity("22AYAR", "GOLD"))

        // Map silver
        rates.add(dto.gumus.toRateEntity("GUMUS", "SILVER"))

        return rates
    }

    private fun CommonRateDto.toRateEntity(code: String, type: String): RateEntity {
        val changePercent = calculateChangePercent()

        return RateEntity(
            id = code,  // Use code as ID so MarketDataManager can find it
            name = name,
            type = type,
            buyPrice = buying,
            sellPrice = selling,
            change = change,
            changePercent = changePercent,
            lastUpdated = Date(),
            isChangePercentPositive = change >= 0
        )
    }

    private fun CommonRateDto.calculateChangePercent(): Double {
        return if (selling > 0) {
            (change / selling) * 100
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
