package com.xptlabs.varliktakibi.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xptlabs.varliktakibi.data.remote.AssetTrackerRemoteDataSource
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AssetTrackerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val remoteDataSource: AssetTrackerRemoteDataSource,
    private val assetRepository: AssetRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "asset_tracker_sync_work"
        const val TAG = "AssetTrackerSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting background sync...")

            // Fetch gold rates
            val goldRatesResult = remoteDataSource.getGoldRates()
            val currencyRatesResult = remoteDataSource.getCurrencyRates()

            var syncedCount = 0

            // Process gold rates
            goldRatesResult.onSuccess { goldRates ->
                Log.d(TAG, "Fetched ${goldRates.size} gold rates")
                syncedCount += goldRates.size
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch gold rates: ${error.message}")
            }

            // Process currency rates
            currencyRatesResult.onSuccess { currencyRates ->
                Log.d(TAG, "Fetched ${currencyRates.size} currency rates")
                syncedCount += currencyRates.size
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch currency rates: ${error.message}")
            }

            // Update asset prices in database
            updateAssetPrices()

            Log.d(TAG, "Background sync completed. Synced $syncedCount rates")

            // Send notification if needed
            if (syncedCount > 0) {
                sendSyncNotification(syncedCount)
            }

            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Background sync failed", exception)
            Result.retry()
        }
    }

    private suspend fun updateAssetPrices() {
        try {
            // Get all user assets
            val assets = assetRepository.getAllAssets()

            // TODO: Update each asset's current price with latest market data
            // This will be implemented when we have the rate repository

            Log.d(TAG, "Asset prices updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update asset prices", e)
        }
    }

    private fun sendSyncNotification(syncedCount: Int) {
        // TODO: Send notification about successful sync
        // Will be implemented with notification manager
        Log.d(TAG, "Would send notification: $syncedCount rates synced")
    }
}