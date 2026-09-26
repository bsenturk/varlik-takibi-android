package com.xptlabs.varliktakibi.data.repo

import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.category

/**
 * Pro aboneliği bittiğinde geriye dönük kilit kuralları. iOS `ProLock.swift` portu.
 *
 * Kapılar eskiden yalnızca *oluşturma anındaydı* ([canCreatePortfolio],
 * `AddAssetViewModel.openCategory`): aboneliği biten kullanıcı Pro'yken açtığı
 * portföyleri ve eklediği fonları sınırsızca görmeye devam ediyordu. Kural tek
 * yerde duruyor ki her tüketici (portföy, analiz, varlık ekleme) aynı cevabı
 * versin.
 *
 * Veri **asla** silinmez, yalnızca erişim kısıtlanır; Pro'ya dönüldüğünde her
 * şey olduğu gibi geri gelir.
 */
object ProLock {

    /**
     * Pro değilken kilitlenen portföylerin id'leri.
     *
     * En eski [PortfolioRepository.FREE_PORTFOLIO_LIMIT] tanesi açık kalır:
     * `createdAt` deterministik ve kullanıcının elle değiştiremeyeceği tek alan
     * — `sortOrder` sürüklenerek değiştirilebildiği için kilit "oynatılabilir"
     * olurdu. "Genel" bir toplam görünümü, asla kilitlenmez.
     */
    fun lockedPortfolioIds(portfolios: List<PortfolioEntity>, isPro: Boolean): Set<String> {
        if (isPro) return emptySet()
        return portfolios
            .filter { !it.isGeneral }
            .sortedBy { it.createdAt }
            .drop(PortfolioRepository.FREE_PORTFOLIO_LIMIT)
            .map { it.id }
            .toSet()
    }

    /** Yeni portföy oluşturulabilir mi — oluşturma anındaki eski kapı. */
    fun canCreatePortfolio(portfolios: List<PortfolioEntity>, isPro: Boolean): Boolean =
        isPro || portfolios.count { !it.isGeneral } < PortfolioRepository.FREE_PORTFOLIO_LIMIT

    /**
     * Varlık kilitli mi: premium kategoriden (fon) geliyorsa ya da kilitli bir
     * portföyde duruyorsa. [lockedIds] dışarıdan geçiliyor; liste başına bir kez
     * hesaplanıp her satırda yeniden kurulmasın diye.
     */
    fun isLocked(asset: AssetEntity, lockedIds: Set<String>, isPro: Boolean): Boolean {
        if (isPro) return false
        if (asset.category.isPremium) return true
        return asset.portfolioId in lockedIds
    }

    /**
     * Toplamlara, grafiklere ve analize girecek varlıklar. Kilitli olanlar
     * arayüzde görünmeye devam eder ama hiçbir tutara katılmaz.
     */
    fun unlocked(
        assets: List<AssetEntity>,
        portfolios: List<PortfolioEntity>,
        isPro: Boolean
    ): List<AssetEntity> {
        if (isPro) return assets
        val lockedIds = lockedPortfolioIds(portfolios, isPro)
        return assets.filter { !isLocked(it, lockedIds, isPro) }
    }
}
