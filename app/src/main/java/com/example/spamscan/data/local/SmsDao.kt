package com.example.spamscan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM cached_sms ORDER BY timestamp DESC")
    fun getAllCachedSms(): Flow<List<CachedSms>>

    @Query("SELECT * FROM cached_sms WHERE id = :smsId")
    suspend fun getCachedSmsById(smsId: Long): CachedSms?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(sms: CachedSms)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(smsList: List<CachedSms>)

    @Query("DELETE FROM cached_sms")
    suspend fun clearCache()
}
