package com.xptlabs.varliktakibi.core.model

import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.totalCost
import com.xptlabs.varliktakibi.data.local.entity.totalValue
import kotlin.math.abs

/**
 * Bir varlık kümesinin toplam değeri ve maliyete göre kâr/zararı (hep TL).
 * iOS `PortfolioMetrics.swift` portu.
 */
data class PortfolioMetrics(
    val totalValue: Double,
    val totalCost: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    /** Fiyatı henüz alınamamış varlık var mı — UI kısmi veriyi belirtir. */
    val hasMissingPrices: Boolean
) {
    val hasProfitLoss: Boolean get() = abs(profitLoss) > 0.01
    val isPositive: Boolean get() = profitLoss >= 0

    companion object {
        val ZERO = PortfolioMetrics(0.0, 0.0, 0.0, 0.0, false)

        fun compute(assets: List<AssetEntity>): PortfolioMetrics {
            if (assets.isEmpty()) return ZERO

            var value = 0.0
            var cost = 0.0
            var missing = false

            for (asset in assets) {
                val assetValue = asset.totalValue
                if (assetValue == null) {
                    // Fiyatı bilinmeyen varlığı maliyetiyle de saymıyoruz: değere
                    // katmadan maliyete katmak sahte bir zarar üretirdi.
                    missing = true
                    continue
                }
                value += assetValue
                cost += asset.totalCost
            }

            val profitLoss = value - cost
            return PortfolioMetrics(
                totalValue = value,
                totalCost = cost,
                profitLoss = profitLoss,
                profitLossPercent = if (cost > 0) profitLoss / cost * 100.0 else 0.0,
                hasMissingPrices = missing
            )
        }
    }
}
