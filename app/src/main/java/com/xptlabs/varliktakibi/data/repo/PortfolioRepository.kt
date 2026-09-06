package com.xptlabs.varliktakibi.data.repo

import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.dao.PortfolioDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portföy ve varlık CRUD'u. Tek implementasyonu olduğu için ayrı bir arayüz
 * tanımlanmıyor; test edilmesi gereken hesap mantığı zaten saf fonksiyonlarda
 * (PortfolioMetrics, weightedAverageCost).
 */
@Singleton
class PortfolioRepository @Inject constructor(
    private val portfolioDao: PortfolioDao,
    private val assetDao: AssetDao,
    private val historyDao: HistoryDao
) {

    fun observePortfolios(): Flow<List<PortfolioEntity>> = portfolioDao.observeAll()
    fun observeAssets(): Flow<List<AssetEntity>> = assetDao.observeAll()

    suspend fun portfolios(): List<PortfolioEntity> = portfolioDao.getAll()
    suspend fun assets(): List<AssetEntity> = assetDao.getAll()
    suspend fun assetsIn(portfolioId: String): List<AssetEntity> =
        assetDao.getForPortfolio(portfolioId)

    /**
     * İlk açılışta "Genel" toplayıcı ve bir gerçek portföy ("Portföyüm") olsun.
     * Her açılışta çağrılabilir; var olanı tekrar oluşturmaz.
     */
    suspend fun ensureDefaults() {
        val existing = portfolioDao.getAll()

        if (existing.none { it.isGeneral }) {
            portfolioDao.insert(
                PortfolioEntity(
                    name = GENERAL_NAME,
                    colorHex = PortfolioColor.BLUE.hex,
                    sortOrder = 0,
                    isGeneral = true
                )
            )
        }
        if (existing.none { !it.isGeneral }) {
            portfolioDao.insert(
                PortfolioEntity(
                    name = DEFAULT_PORTFOLIO_NAME,
                    colorHex = PortfolioColor.PURPLE.hex,
                    sortOrder = 1,
                    isGeneral = false
                )
            )
        }
    }

    suspend fun createPortfolio(name: String, color: PortfolioColor): PortfolioEntity {
        val portfolio = PortfolioEntity(
            name = name,
            colorHex = color.hex,
            sortOrder = portfolioDao.maxSortOrder() + 1,
            isGeneral = false
        )
        portfolioDao.insert(portfolio)
        return portfolio
    }

    suspend fun updatePortfolio(portfolio: PortfolioEntity, name: String, color: PortfolioColor) {
        portfolioDao.update(portfolio.copy(name = name, colorHex = color.hex))
    }

    /**
     * Portföyü ve içindeki varlıkları siler. Varlık satırları ve anlık
     * görüntüler foreign key CASCADE ile gidiyor; geçmiş tabloları sembol
     * bazlı olduğu için elle temizleniyor.
     */
    suspend fun deletePortfolio(portfolio: PortfolioEntity) {
        if (portfolio.isGeneral) return
        val owned = assetDao.getForPortfolio(portfolio.id)
        portfolioDao.delete(portfolio)
        owned.forEach { deleteOrphanHistory(it.symbol) }
    }

    suspend fun deleteAsset(asset: AssetEntity) {
        assetDao.delete(asset)
        deleteOrphanHistory(asset.symbol)
    }

    /**
     * Sembol geçmişi portföyler arasında ortak. Aynı sembolü başka bir portföy
     * hâlâ tutuyorsa geçmiş silinmez — iOS burada geçmişi koşulsuz siliyor ve
     * ikinci portföyün grafiğini de götürüyor.
     */
    private suspend fun deleteOrphanHistory(symbol: String) {
        if (assetDao.countBySymbol(symbol) == 0) {
            historyDao.deleteAllForSymbol(symbol)
        }
    }

    companion object {
        const val GENERAL_NAME = "Genel"
        const val DEFAULT_PORTFOLIO_NAME = "Portföyüm"

        /** Ücretsiz kullanıcının tutabileceği gerçek (Genel hariç) portföy sayısı. */
        const val FREE_PORTFOLIO_LIMIT = 2
    }
}
