package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.totalValue
import com.xptlabs.varliktakibi.data.repo.AssetEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Para hesabı yapan saf mantık — bozulursa kullanıcı yanlış kâr/zarar görür. */
class PortfolioMathTest {

    private fun asset(
        amount: Double,
        cost: Double,
        price: Double?,
        symbol: String = "GRAM_ALTIN"
    ) = AssetEntity(
        portfolioId = "p1",
        type = AssetType.GOLD.id,
        symbol = symbol,
        name = "Gram Altın",
        unit = "gram",
        amount = amount,
        costBasis = cost,
        currentPrice = price
    )

    // ── Ağırlıklı ortalama maliyet ───────────────────────────────────────────

    @Test
    fun `agirlikli ortalama iki alimi dogru harmanlar`() {
        // 10 gram @ 2000 + 10 gram @ 3000 → 20 gram @ 2500
        val result = AssetEditor.weightedAverageCost(10.0, 2000.0, 10.0, 3000.0)
        assertEquals(2500.0, result, 1e-9)
    }

    @Test
    fun `agirlikli ortalama miktara gore agirliklandirir`() {
        // 1 gram @ 1000 + 9 gram @ 2000 → 1900, 1500 değil.
        val result = AssetEditor.weightedAverageCost(1.0, 1000.0, 9.0, 2000.0)
        assertEquals(1900.0, result, 1e-9)
    }

    @Test
    fun `toplam miktar sifirken mevcut maliyet korunur`() {
        // Sıfıra bölünüp NaN üretmemeli.
        val result = AssetEditor.weightedAverageCost(0.0, 1500.0, 0.0, 3000.0)
        assertEquals(1500.0, result, 1e-9)
        assertFalse(result.isNaN())
    }

    // ── Portföy metrikleri ───────────────────────────────────────────────────

    @Test
    fun `kar dogru hesaplanir`() {
        val metrics = PortfolioMetrics.compute(listOf(asset(10.0, 2000.0, 2500.0)))
        assertEquals(25_000.0, metrics.totalValue, 1e-9)
        assertEquals(20_000.0, metrics.totalCost, 1e-9)
        assertEquals(5_000.0, metrics.profitLoss, 1e-9)
        assertEquals(25.0, metrics.profitLossPercent, 1e-9)
        assertTrue(metrics.isPositive)
    }

    @Test
    fun `zarar negatif yuzde uretir`() {
        val metrics = PortfolioMetrics.compute(listOf(asset(10.0, 3000.0, 2400.0)))
        assertEquals(-6_000.0, metrics.profitLoss, 1e-9)
        assertEquals(-20.0, metrics.profitLossPercent, 1e-9)
        assertFalse(metrics.isPositive)
    }

    @Test
    fun `sifir maliyet yuzdeyi sonsuza gondermez`() {
        val metrics = PortfolioMetrics.compute(listOf(asset(5.0, 0.0, 100.0)))
        assertEquals(0.0, metrics.profitLossPercent, 1e-9)
        assertFalse(metrics.profitLossPercent.isNaN())
    }

    @Test
    fun `fiyati bilinmeyen varlik ne degere ne maliyete sayilir`() {
        // Yalnızca maliyete sayılsaydı sahte bir zarar üretirdi.
        val metrics = PortfolioMetrics.compute(
            listOf(
                asset(10.0, 2000.0, 2500.0),
                asset(5.0, 1000.0, null, symbol = "BTC")
            )
        )
        assertEquals(25_000.0, metrics.totalValue, 1e-9)
        assertEquals(20_000.0, metrics.totalCost, 1e-9)
        assertEquals(5_000.0, metrics.profitLoss, 1e-9)
        assertTrue("Eksik fiyat bayrağı kalkmalı", metrics.hasMissingPrices)
    }

    @Test
    fun `bos portfoy sifir dondurur`() {
        val metrics = PortfolioMetrics.compute(emptyList())
        assertEquals(PortfolioMetrics.ZERO, metrics)
        assertFalse(metrics.hasProfitLoss)
    }

    @Test
    fun `fiyati olmayan varligin degeri null`() {
        assertNull(asset(3.0, 100.0, null).totalValue)
    }

    // ── Biçimleme ────────────────────────────────────────────────────────────

    @Test
    fun `tutar turkce bicimde yazilir`() {
        assertEquals("12.345,68 ₺", TrFormat.money(12345.678))
    }

    @Test
    fun `kucuk ama sifir olmayan yuzde sifir gibi gosterilmez`() {
        // "%0,00" kullanıcıya kâr yokmuş gibi görünüyordu.
        assertEquals("+<0,01%", TrFormat.percent(0.004))
        assertEquals("-<0,01%", TrFormat.percent(-0.004))
    }

    @Test
    fun `tam sayi miktarlar ondaliksiz yazilir`() {
        assertEquals("10", TrFormat.amount(10.0))
        assertEquals("0,0002", TrFormat.amount(0.0002))
    }
}
