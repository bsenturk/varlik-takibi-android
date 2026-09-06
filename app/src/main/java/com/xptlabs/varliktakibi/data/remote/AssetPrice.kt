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
    val source: String? = null,
    @SerialName("updated_at") val updatedAt: String
)
