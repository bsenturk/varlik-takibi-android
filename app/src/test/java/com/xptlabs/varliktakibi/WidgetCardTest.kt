package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.remote.AssetPrice
import com.xptlabs.varliktakibi.widget.WidgetState
import com.xptlabs.varliktakibi.widget.buildWidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCardTest {

    private val general = PortfolioEntity(id = "g", name = "Genel", colorHex = "#007AFF", sortOrder = 0, isGeneral = true)
    private val mine = PortfolioEntity(id = "p", name = "Portföyüm", colorHex = "#AF52DE", sortOrder = 1, isGeneral = false)
    private val gold = AssetEntity(
        portfolioId = "p", type = AssetType.GOLD.id, symbol = "GRAM_ALTIN", name = "Gram Altın",
        unit = "gram", amount = 10.0, costBasis = 5_000.0, currentPrice = 6_000.0, location = "Ev"
    )
    private fun price(symbol: String, value: Double, currency: String = "TRY") =
        AssetPrice(symbol = symbol, currency = currency, assetType = "gold", price = value, updatedAt = "2026-09-26T00:00:00Z")

    private fun state(isPro: Boolean, prices: List<AssetPrice>?, assets: List<AssetEntity> = listOf(gold)) =
        buildWidgetState(listOf(general, mine), assets, "p", emptySet(), Currency.TRY, isPro, prices)

    @Test
    fun `pro kart canli fiyatla hesaplanir`() {
        val card = (state(true, listOf(price("GRAM_ALTIN", 7_000.0))) as WidgetState.Ready).card
        assertEquals(TrFormat.money(70_000.0), card.total)
        assertEquals("Gram Altın · Ev", card.rows.single().name)
        assertEquals(40.0, card.percent, 1e-9)
        assertTrue(!card.stale)
    }

    @Test
    fun `fiyat cekilemezse son bilinen fiyat ve bayat notu`() {
        val card = (state(true, null) as WidgetState.Ready).card
        assertEquals(TrFormat.money(60_000.0), card.total)
        assertTrue(card.stale)
    }

    @Test
    fun `pro degilse kendi kart maskeli, bos portfoyde jenerik kilit`() {
        val locked = state(false, emptyList()) as WidgetState.Locked
        assertEquals(TrFormat.MASK, locked.card!!.total)
        assertTrue(locked.card!!.isLocked)
        assertEquals(WidgetState.Locked(null), state(false, emptyList(), assets = emptyList()))
    }
}
