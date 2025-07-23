package com.xptlabs.varliktakibi.data.local.backup

import android.content.Context
import com.xptlabs.varliktakibi.data.local.database.AssetTrackerDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AssetTrackerDatabase
) {

    suspend fun exportUserData(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Basit JSON export - gelecekte kullanıcılar verilerini koruyabilir
            val assets = database.assetDao().getAllAssets().first()

            val exportData = mapOf(
                "assets" to assets.map { asset ->
                    mapOf(
                        "type" to asset.type,
                        "name" to asset.name,
                        "amount" to asset.amount,
                        "purchasePrice" to asset.purchasePrice,
                        "dateAdded" to asset.dateAdded.time
                    )
                },
                "exportDate" to System.currentTimeMillis(),
                "appVersion" to com.xptlabs.varliktakibi.BuildConfig.VERSION_NAME
            )

            val gson = com.google.gson.Gson()
            Result.success(gson.toJson(exportData))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
