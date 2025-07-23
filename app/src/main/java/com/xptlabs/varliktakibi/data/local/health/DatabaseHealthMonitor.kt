package com.xptlabs.varliktakibi.data.local.health

import com.xptlabs.varliktakibi.data.local.database.AssetTrackerDatabase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseHealthMonitor @Inject constructor(
    private val database: AssetTrackerDatabase
) {

    suspend fun checkDatabaseHealth(): DatabaseHealth {
        return try {
            val assetCount = database.assetDao().getAssetCount()
            val rateCount = database.rateDao().getAllRates().first().size

            DatabaseHealth(
                isHealthy = true,
                assetCount = assetCount,
                rateCount = rateCount,
                lastCheckTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            DatabaseHealth(
                isHealthy = false,
                error = e.message,
                lastCheckTime = System.currentTimeMillis()
            )
        }
    }

    data class DatabaseHealth(
        val isHealthy: Boolean,
        val assetCount: Int = 0,
        val rateCount: Int = 0,
        val error: String? = null,
        val lastCheckTime: Long
    )
}
