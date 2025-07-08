package com.xptlabs.varliktakibi.data.local.dao

import androidx.room.*
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface RateDao {

    @Query("SELECT * FROM rates ORDER BY lastUpdated DESC")
    fun getAllRates(): Flow<List<RateEntity>>

    @Query("SELECT * FROM rates WHERE type = :type ORDER BY lastUpdated DESC")
    fun getRatesByType(type: String): Flow<List<RateEntity>>

    @Query("SELECT * FROM rates WHERE id = :id")
    suspend fun getRateById(id: String): RateEntity?

    @Query("SELECT * FROM rates WHERE type = 'GOLD' ORDER BY lastUpdated DESC")
    fun getGoldRates(): Flow<List<RateEntity>>

    @Query("SELECT * FROM rates WHERE type = 'CURRENCY' ORDER BY lastUpdated DESC")
    fun getCurrencyRates(): Flow<List<RateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: RateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<RateEntity>)

    @Update
    suspend fun updateRate(rate: RateEntity)

    @Delete
    suspend fun deleteRate(rate: RateEntity)

    @Query("DELETE FROM rates WHERE type = :type")
    suspend fun deleteRatesByType(type: String)

    @Query("DELETE FROM rates")
    suspend fun deleteAllRates()

    @Query("SELECT lastUpdated FROM rates ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLastUpdateTime(): Date?
}