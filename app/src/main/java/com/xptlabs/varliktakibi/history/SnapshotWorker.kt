package com.xptlabs.varliktakibi.history

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.model.PortfolioMetrics
import com.xptlabs.varliktakibi.data.local.dao.SnapshotDao
import com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import com.xptlabs.varliktakibi.market.MarketDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Günde bir kez fiyatları çeker ve her varlık + portföy için o günün anlık
 * görüntüsünü yazar.
 *
 * iOS'ta anlık görüntü yalnızca uygulama ön plana geldiğinde kaydediliyor, bu
 * yüzden uygulamayı bir hafta açmayan kullanıcının grafiğinde bir haftalık
 * boşluk oluşuyor (Time Machine ancak geçmiş fiyat bulabilirse dolduruyor).
 * Android'de bu iş bedava geliyor.
 */
@HiltWorker
class SnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val market: MarketDataStore,
    private val repository: PortfolioRepository,
    private val history: HistoryRecorder,
    private val snapshotDao: SnapshotDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            market.refresh()

            val today = Days.today()
            val assets = repository.assets()
            if (assets.isEmpty()) return Result.success()

            // Varlık başına günlük fiyat anlık görüntüsü.
            val priced = assets.map { asset ->
                val price = market.tryPrice(asset.symbol)
                if (price != null) history.recordPrice(asset.symbol, today, price, asset.amount)
                asset.copy(currentPrice = price ?: asset.currentPrice)
            }

            // Portföy başına gün sonu toplam değeri.
            val snapshots = repository.portfolios()
                .filter { !it.isGeneral }
                .mapNotNull { portfolio ->
                    val owned = priced.filter { it.portfolioId == portfolio.id }
                    val total = PortfolioMetrics.compute(owned).totalValue
                    // 0 yazmak grafiği tabana çakar; değerlenemeyen günü atla.
                    if (total <= 0) null
                    else PortfolioSnapshotEntity(
                        portfolioId = portfolio.id,
                        day = today,
                        totalValue = total
                    )
                }

            if (snapshots.isNotEmpty()) snapshotDao.upsertAll(snapshots)
            Log.d(TAG, "Snapshot yazıldı: ${snapshots.size} portföy, ${assets.size} varlık")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Snapshot işi başarısız", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_snapshot"
        private const val TAG = "SnapshotWorker"
    }
}

@Singleton
class SnapshotScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<SnapshotWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SnapshotWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
