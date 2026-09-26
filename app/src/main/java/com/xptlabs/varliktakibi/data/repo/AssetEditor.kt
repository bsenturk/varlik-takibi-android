package com.xptlabs.varliktakibi.data.repo

import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.assetType
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
     * Enstrümanı portföye ekler. Aynı sembol o portföyde **aynı yerde** zaten
     * varsa miktarlar toplanır ve maliyet ağırlıklı ortalamaya çekilir —
     * "evde 10 gram" ile "bankada 20 gram" ayrı varlıklar.
     *
     * @param costPerUnit kullanıcının girdiği alış fiyatı; boş bırakıldıysa
     *   güncel piyasa fiyatı kullanılır (kâr/zarar sıfırdan başlar).
     * @return true ise mevcut varlıkla birleştirildi.
     */
    suspend fun addOrMerge(
        instrument: Instrument,
        portfolioId: String,
        amount: Double,
        costPerUnit: Double?,
        location: String = ""
    ): Boolean {
        require(amount > 0) { "Miktar sıfırdan büyük olmalı" }

        val place = normalizedLocation(location)
        val price = market.tryPrice(instrument.symbol) ?: instrument.priceTry
        val cost = costPerUnit?.takeIf { it > 0 } ?: price
        val existing = assetDao.findInPortfolio(portfolioId, instrument.symbol, place)

        if (existing == null) {
            val asset = AssetEntity(
                portfolioId = portfolioId,
                type = instrument.type.id,
                symbol = instrument.symbol,
                name = instrument.name,
                unit = instrument.unit,
                amount = amount,
                costBasis = cost,
                currentPrice = price,
                location = place
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
     * Elle değer girilen varlık (ev, araba, BES…) ekler. Temsil: miktar hep 1,
     * fiyat = girilen değer — toplam, kâr/zarar ve kategori toplamı mevcut
     * hesapla çalışıyor. Her biri kendine özel sembol taşıyor ve hiç
     * birleştirilmiyor: her ev ayrı bir varlık.
     *
     * @param cost opsiyonel alış fiyatı / yatırılan tutar; yoksa değer (kâr/zarar 0).
     */
    suspend fun addManual(
        type: AssetType,
        portfolioId: String,
        value: Double,
        cost: Double?,
        name: String
    ) {
        require(type.isManual) { "Elle girilen tür değil" }
        require(value > 0) { "Değer sıfırdan büyük olmalı" }

        val id = java.util.UUID.randomUUID().toString()
        val costBasis = cost?.takeIf { it > 0 } ?: value
        val asset = AssetEntity(
            id = id,
            portfolioId = portfolioId,
            type = type.id,
            symbol = AssetType.manualSymbol(id),
            name = name.trim().take(MAX_NAME_LENGTH).ifEmpty { type.displayName },
            unit = type.unit,
            amount = 1.0,
            costBasis = costBasis,
            currentPrice = value
        )
        assetDao.insert(asset)
        history.recordInitial(asset, costBasis, snapshotPrice = value)
    }

    /**
     * Elle girilen varlığın değerini ve adını günceller. Değer geçmişine bugünün
     * noktası yazılır; işlem geçmişinde "Güncelleme" olarak görünür.
     */
    suspend fun setManualValue(asset: AssetEntity, value: Double, name: String) {
        require(value > 0) { "Değer sıfırdan büyük olmalı" }
        val type = asset.assetType
        val updated = asset.copy(
            currentPrice = value,
            name = name.trim().take(MAX_NAME_LENGTH).ifEmpty { type.displayName },
            lastUpdated = System.currentTimeMillis()
        )
        assetDao.update(updated)
        if (value != asset.currentPrice) {
            history.recordTransaction(updated, TransactionType.EDIT, delta = 0.0, price = value)
        }
        history.recordDailySnapshot(updated)
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
            // O günün piyasa fiyatı: geçmiş ekranı "o gün kaça alındı / satıldı"yı
            // bununla gösteriyor. Fiyat yoksa maliyet.
            price = updated.currentPrice ?: asset.costBasis
        )
        history.recordDailySnapshot(updated)
    }

    /** Yeri değiştirir. Aynı yerde aynı enstrüman varsa birleştirilmez (iOS ile aynı). */
    suspend fun setLocation(asset: AssetEntity, location: String) {
        val place = normalizedLocation(location)
        if (place == asset.location) return
        assetDao.update(asset.copy(location = place, lastUpdated = System.currentTimeMillis()))
    }

    /** Birim başına maliyeti doğrudan düzenler (kullanıcı yanlış girdiyse). */
    suspend fun setCostBasis(asset: AssetEntity, costPerUnit: Double) {
        require(costPerUnit > 0) { "Alış fiyatı sıfırdan büyük olmalı" }
        assetDao.update(
            asset.copy(costBasis = costPerUnit, lastUpdated = System.currentTimeMillis())
        )
    }

    companion object {
        const val MAX_LOCATION_LENGTH = 30
        const val MAX_NAME_LENGTH = 40

        /** Karşılaştırma ve kayıt aynı temizlenmiş biçimi kullanıyor. */
        fun normalizedLocation(raw: String): String = raw.trim().take(MAX_LOCATION_LENGTH)

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
