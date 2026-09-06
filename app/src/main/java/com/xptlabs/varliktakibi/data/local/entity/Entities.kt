package com.xptlabs.varliktakibi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xptlabs.varliktakibi.core.model.AssetCategory
import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import java.util.UUID

@Entity(
    tableName = "portfolios",
    indices = [Index("sortOrder")]
)
data class PortfolioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** [com.xptlabs.varliktakibi.core.model.PortfolioColor] hex değeri. */
    val colorHex: String,
    val sortOrder: Int,
    /** Silinemeyen "Genel" portföy: her varlığı kategori bazında toplar. */
    val isGeneral: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("portfolioId"), Index("symbol")]
)
data class AssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val portfolioId: String,
    /** [com.xptlabs.varliktakibi.core.model.AssetType.id] */
    val type: String,
    /** `assets_prices.symbol` ile aynı — fiyat ve geçmiş aramasının tek anahtarı. */
    val symbol: String,
    val name: String,
    val unit: String,
    val amount: Double,
    /**
     * Birim başına ağırlıklı ortalama maliyet (TL). iOS bunu UserDefaults'ta ayrı
     * bir haritada tutuyor ve senkronu elle yönetiyor; burada varlığın kendi
     * kolonu — tek kaynak, kayıp riski yok.
     */
    val costBasis: Double,
    /** Son bilinen piyasa fiyatı (TL). Fiyat hiç alınamadıysa null. */
    val currentPrice: Double?,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Varlığın günlük fiyat anlık görüntüsü. `symbol` bazlı — kripto/hisse tek bir
 * jenerik [com.xptlabs.varliktakibi.core.model.AssetType] paylaştığı için tür
 * bazlı anahtar yeterli olmaz.
 */
@Entity(
    tableName = "price_history",
    indices = [Index(value = ["symbol", "day"], unique = true), Index("day")]
)
data class PriceHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    /** Gün başlangıcı (epoch ms), yerel saat dilimine göre. */
    val day: Long,
    val price: Double,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType { INITIAL, ADD, REMOVE, EDIT }

@Entity(
    tableName = "transaction_history",
    indices = [Index("symbol"), Index("date")]
)
data class TransactionHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val date: Long,
    val transactionType: TransactionType,
    /** İşlemin kendi miktarı (eklenen/çıkarılan). */
    val amount: Double,
    /** İşlemden sonraki toplam miktar — Time Machine bunu geri oynatıyor. */
    val totalAmount: Double,
    val price: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/** Portföyün gün sonu toplam değeri (TL). Analiz grafiğinin veri kaynağı. */
@Entity(
    tableName = "portfolio_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["portfolioId", "day"], unique = true)]
)
data class PortfolioSnapshotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val portfolioId: String,
    val day: Long,
    val totalValue: Double
)

// ── Hesap uzantıları ─────────────────────────────────────────────────────────
// Entity'lerin yanına ayrı bir domain modeli + mapper koymuyoruz: entity zaten
// düz bir data class ve canlı fiyat iOS'taki gibi satıra geri yazılıyor, yani
// entity aynı zamanda görüntü modeli. Türetilmiş değerler uzantı olarak burada.

val PortfolioEntity.color: PortfolioColor get() = PortfolioColor.fromHex(colorHex)

val AssetEntity.assetType: AssetType
    get() = AssetType.fromId(type) ?: AssetType.GOLD

val AssetEntity.category: AssetCategory get() = assetType.category

/** Güncel piyasa değeri (TL). Fiyat henüz alınamadıysa null. */
val AssetEntity.totalValue: Double?
    get() = currentPrice?.let { amount * it }

/** Ağırlıklı ortalama maliyete göre toplam yatırım (TL). */
val AssetEntity.totalCost: Double get() = amount * costBasis

/** Maliyete göre kâr/zarar (TL). Fiyat yoksa null — sıfır göstermek yanıltıcı olur. */
val AssetEntity.profitLoss: Double?
    get() = totalValue?.minus(totalCost)

val AssetEntity.profitLossPercent: Double
    get() {
        val pl = profitLoss ?: return 0.0
        return if (totalCost > 0) pl / totalCost * 100.0 else 0.0
    }
