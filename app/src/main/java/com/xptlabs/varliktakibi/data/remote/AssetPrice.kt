package com.xptlabs.varliktakibi.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `assets_prices` tablosunun bir satırı. Tablo `(symbol, currency)` ile
 * anahtarlı — aynı enstrüman birden fazla para biriminde bulunabilir
 * (kripto hem USD hem TRY). iOS `SupabaseModels.swift` karşılığı.
 */
@Serializable
data class AssetPrice(
    val symbol: String,
    val currency: String,
    val name: String? = null,
    /** Backend kategorisi: crypto, currency, gold, bist, us_stock, fund. */
    @SerialName("asset_type") val assetType: String,
    val price: Double,
    @SerialName("change_percent") val changePercent: Double? = null,
    /**
     * Alış fiyatı — yalnızca altın/dövizde (Truncgil makas yayımlıyor); kripto,
     * hisse ve fonda makas kavramı yok, null.
     */
    @SerialName("buy_price") val buyPrice: Double? = null,
    val source: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    /** Enstrümanın logosu (kripto/hisse); Storage'daki kopya. Yoksa null. */
    @SerialName("logo_url") val logoUrl: String? = null
)
