package com.xptlabs.varliktakibi.data.repo

import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionType
import com.xptlabs.varliktakibi.history.HistoryRecorder
import com.xptlabs.varliktakibi.market.Instrument
import com.xptlabs.varliktakibi.market.MarketDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Varlık ekleme / miktar güncelleme. Ayrı bir sınıf çünkü üç ekran (ekleme
 * akışı, düzenleme sayfası, onboarding devri) aynı birleştirme ve maliyet
 * mantığına dokunuyor ve bu mantığın tek bir doğru hâli olmalı.
 *
 * iOS `AddAssetSheet.save(instrument:)` + `PortfolioManager.updatePurchasePrice`
 * karşılığı.
 */
@Singleton
class AssetEditor @Inject constructor(
    private val assetDao: AssetDao,
    private val history: HistoryRecorder,
    private val market: MarketDataStore
) {

    /**
     * Enstrümanı portföye ekler. Aynı sembol o portföyde zaten varsa miktarlar
     * toplanır ve maliyet ağırlıklı ortalamaya çekilir.
     *
     * @param costPerUnit kullanıcının girdiği alış fiyatı; boş bırakıldıysa
     *   güncel piyasa fiyatı kullanılır (kâr/zarar sıfırdan başlar).
     * @return true ise mevcut varlıkla birleştirildi.
     */
    suspend fun addOrMerge(
        instrument: Instrument,
        portfolioId: String,
        amount: Double,
        costPerUnit: Double?
    ): Boolean {
        require(amount > 0) { "Miktar sıfırdan büyük olmalı" }

        val price = market.tryPrice(instrument.symbol) ?: instrument.priceTry
        val cost = costPerUnit?.takeIf { it > 0 } ?: price
        val existing = assetDao.findInPortfolio(portfolioId, instrument.symbol)

        if (existing == null) {
            val asset = AssetEntity(
                portfolioId = portfolioId,
                type = instrument.type.id,
                symbol = instrument.symbol,
                name = instrument.name,
                unit = instrument.unit,
                amount = amount,
                costBasis = cost,
                currentPrice = price
            )
            assetDao.insert(asset)
            history.recordInitial(asset, cost)
            return false
        }

        val merged = existing.copy(
            amount = existing.amount + amount,
            costBasis = weightedAverageCost(
                existingAmount = existing.amount,
                existingCost = existing.costBasis,
                addedAmount = amount,
                addedCost = cost
            ),
            currentPrice = price,
            lastUpdated = System.currentTimeMillis()
        )
        assetDao.update(merged)
        history.recordTransaction(merged, TransactionType.ADD, delta = amount, price = cost)
        history.recordDailySnapshot(merged)
        return true
    }

    /**
     * Miktarı doğrudan ayarlar (düzenleme sayfası). Maliyet birim başına aynı
     * kalır — kullanıcı miktarı düzeltiyor, yeni alım yapmıyor.
     */
    suspend fun setAmount(asset: AssetEntity, newAmount: Double) {
        require(newAmount > 0) { "Miktar sıfırdan büyük olmalı" }

        val updated = asset.copy(
            amount = newAmount,
            currentPrice = market.tryPrice(asset.symbol) ?: asset.currentPrice,
            lastUpdated = System.currentTimeMillis()
        )
        assetDao.update(updated)

        val type = when {
            newAmount > asset.amount -> TransactionType.ADD
            newAmount < asset.amount -> TransactionType.REMOVE
            else -> TransactionType.EDIT
        }
        history.recordTransaction(
            asset = updated,
            type = type,
            delta = kotlin.math.abs(newAmount - asset.amount),
            price = asset.costBasis
        )
        history.recordDailySnapshot(updated)
    }

    /** Birim başına maliyeti doğrudan düzenler (kullanıcı yanlış girdiyse). */
    suspend fun setCostBasis(asset: AssetEntity, costPerUnit: Double) {
        require(costPerUnit > 0) { "Alış fiyatı sıfırdan büyük olmalı" }
        assetDao.update(
            asset.copy(costBasis = costPerUnit, lastUpdated = System.currentTimeMillis())
        )
    }

    companion object {
        /**
         * Ağırlıklı ortalama birim maliyet. Toplam miktar sıfırsa (olmaması
         * gereken durum) mevcut maliyet korunur, sıfıra bölmek yerine.
         */
        fun weightedAverageCost(
            existingAmount: Double,
            existingCost: Double,
            addedAmount: Double,
            addedCost: Double
        ): Double {
            val total = existingAmount + addedAmount
            if (total <= 0) return existingCost
            return (existingAmount * existingCost + addedAmount * addedCost) / total
        }
    }
}
