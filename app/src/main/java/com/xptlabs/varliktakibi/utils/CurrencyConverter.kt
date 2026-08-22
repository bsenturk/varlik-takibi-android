package com.xptlabs.varliktakibi.utils

import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.domain.models.Currency

/**
 * Currency conversion utility
 * Converts TRY amounts to target currencies using market rates
 */
object CurrencyConverter {

    /**
     * Convert amount from TRY to target currency
     * @param amountInTRY Amount in Turkish Lira
     * @param targetCurrency Target currency to convert to
     * @param currencyRates List of current currency rates
     * @return Converted amount in target currency
     */
    fun convertToTargetCurrency(
        amountInTRY: Double,
        targetCurrency: Currency,
        currencyRates: List<RateEntity>
    ): Double {
        // If target is TRY, no conversion needed
        if (targetCurrency == Currency.TRY) {
            return amountInTRY
        }

        // Find the rate for target currency
        val rate = currencyRates.firstOrNull { it.id == targetCurrency.code || it.name.contains(targetCurrency.displayName) }

        // Use sell price for conversion (TRY to foreign currency)
        val sellPrice = rate?.sellPrice ?: return amountInTRY

        // Formula: Amount in TRY ÷ Target Currency Sell Rate = Amount in Target Currency
        return if (sellPrice > 0) {
            amountInTRY / sellPrice
        } else {
            amountInTRY
        }
    }

    /**
     * Format amount with currency symbol
     */
    fun formatWithCurrency(amount: Double, currency: Currency): String {
        return when (currency) {
            Currency.TRY -> String.format("%,.2f ₺", amount)
            Currency.USD -> String.format("$%,.2f", amount)
            Currency.EUR -> String.format("€%,.2f", amount)
            Currency.GBP -> String.format("£%,.2f", amount)
        }
    }

    /**
     * Get exchange rate for display
     */
    fun getExchangeRate(
        targetCurrency: Currency,
        currencyRates: List<RateEntity>
    ): Double {
        if (targetCurrency == Currency.TRY) return 1.0

        val rate = currencyRates.firstOrNull {
            it.id == targetCurrency.code || it.name.contains(targetCurrency.displayName)
        }

        return rate?.sellPrice ?: 1.0
    }
}
