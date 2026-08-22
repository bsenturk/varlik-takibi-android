package com.xptlabs.varliktakibi.history

import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Varlıkların günlük fiyat anlık görüntüsü ve işlem geçmişi. Sembol bazlı —
 * kripto/hisse tek bir jenerik tür paylaştığı için tür bazlı anahtar yetmez.
 *
 * iOS `AssetHistoryManager.swift` portu.
 */
@Singleton
class HistoryRecorder @Inject constructor(
    private val dao: HistoryDao
) {

    /**
     * Bugünün fiyatını kaydeder; aynı gün için kayıt varsa üzerine yazar.
     * Fiyatı bilinmeyen varlık atlanır — 0 yazmak grafiği tabana çakar.
     */
    suspend fun recordDailySnapshot(asset: AssetEntity, day: Long = Days.today()) {
        val price = asset.currentPrice ?: return
        recordPrice(asset.symbol, day, price, asset.amount)
    }

    suspend fun recordPrice(symbol: String, day: Long, price: Double, amount: Double) {
        val existing = dao.priceOn(symbol, day)
        dao.upsertPrice(
            existing?.copy(price = price, amount = amount, createdAt = System.currentTimeMillis())
                ?: PriceHistoryEntity(symbol = symbol, day = day, price = price, amount = amount)
        )
        if (existing == null) dao.trimPriceHistory(symbol, MAX_PRICE_HISTORY)
    }

    /** Varlık ilk eklendiğinde: alış fiyatını çıpa olarak geçmişe yaz. */
    suspend fun recordInitial(asset: AssetEntity, costPerUnit: Double) {
        val day = Days.startOf(asset.dateAdded)
        recordPrice(asset.symbol, day, costPerUnit, asset.amount)
        dao.insertTransaction(
            TransactionHistoryEntity(
                symbol = asset.symbol,
                date = asset.dateAdded,
                transactionType = TransactionType.INITIAL,
                amount = asset.amount,
                totalAmount = asset.amount,
                price = costPerUnit
            )
        )
    }

    suspend fun recordTransaction(
        asset: AssetEntity,
        type: TransactionType,
        delta: Double,
        price: Double
    ) {
        dao.insertTransaction(
            TransactionHistoryEntity(
                symbol = asset.symbol,
                date = System.currentTimeMillis(),
                transactionType = type,
                amount = delta,
                totalAmount = asset.amount,
                price = price
            )
        )
        dao.trimTransactions(asset.symbol, MAX_TRANSACTIONS)
    }

    suspend fun priceHistory(symbol: String): List<PriceHistoryEntity> = dao.priceHistory(symbol)

    suspend fun transactions(symbol: String): List<TransactionHistoryEntity> =
        dao.transactions(symbol)

    /**
     * Bugünden önceki son bilinen fiyat — "günlük değişim" için referans.
     * Yoksa null (yeni eklenen varlıkta değişim gösterilmez).
     */
    suspend fun previousClose(symbol: String, before: Long = Days.today()): Double? =
        dao.priceHistory(symbol).lastOrNull { it.day < before }?.price

    private companion object {
        /** ~13 ay günlük kayıt: 1Y aralığının dolabilmesi için. */
        const val MAX_PRICE_HISTORY = 400
        const val MAX_TRANSACTIONS = 50
    }
}
