package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.model.AssetType
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.repo.ProLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Abonelik bittikten sonraki erişim kuralları. Bozulursa aboneliği biten
 * kullanıcı Pro içeriğini görmeye devam eder — ya da parasını ödeyen kullanıcı
 * kendi verisinden kilitlenir.
 */
class ProLockTest {

    private fun portfolio(name: String, createdAt: Long, sortOrder: Int, general: Boolean = false) =
        PortfolioEntity(
            id = name,
            name = name,
            colorHex = PortfolioColor.BLUE.hex,
            sortOrder = sortOrder,
            isGeneral = general,
            createdAt = createdAt
        )

    private fun asset(portfolioId: String, type: AssetType) = AssetEntity(
        portfolioId = portfolioId,
        type = type.id,
        symbol = type.supabaseSymbol ?: "X",
        name = type.displayName,
        unit = "adet",
        amount = 1.0,
        costBasis = 1.0,
        currentPrice = 1.0
    )

    private val general = portfolio("Genel", 0, 0, general = true)
    private val old = portfolio("Eski", 100, 2)
    private val mid = portfolio("Orta", 200, 1)
    // sortOrder kasten ters: kilit sürüklemeyle oynatılamamalı.
    private val new = portfolio("Yeni", 300, 0)
    private val all = listOf(general, new, mid, old)

    @Test
    fun `en eski iki portfoy acik kalir`() {
        assertEquals(setOf(new.id), ProLock.lockedPortfolioIds(all, isPro = false))
    }

    @Test
    fun `pro kullanicida hicbir portfoy kilitlenmez`() {
        assertTrue(ProLock.lockedPortfolioIds(all, isPro = true).isEmpty())
    }

    @Test
    fun `olusturma kapisi ucretsiz limitte kapanir`() {
        assertFalse(ProLock.canCreatePortfolio(all, isPro = false))
        assertTrue(ProLock.canCreatePortfolio(listOf(general, old), isPro = false))
        assertTrue(ProLock.canCreatePortfolio(all, isPro = true))
    }

    @Test
    fun `fon acik portfoyde bile kilitli, pro'da degil`() {
        val fund = asset(old.id, AssetType.FUND)
        val locked = ProLock.lockedPortfolioIds(all, isPro = false)
        assertTrue(ProLock.isLocked(fund, locked, isPro = false))
        assertFalse(ProLock.isLocked(fund, emptySet(), isPro = true))
    }

    @Test
    fun `kilitli portfoydeki varlik toplamlara girmez`() {
        val visible = asset(old.id, AssetType.GOLD)
        val hidden = asset(new.id, AssetType.GOLD)
        val fund = asset(mid.id, AssetType.FUND)

        val unlocked = ProLock.unlocked(listOf(visible, hidden, fund), all, isPro = false)
        assertEquals(listOf(visible), unlocked)
        assertEquals(3, ProLock.unlocked(listOf(visible, hidden, fund), all, isPro = true).size)
    }
}
