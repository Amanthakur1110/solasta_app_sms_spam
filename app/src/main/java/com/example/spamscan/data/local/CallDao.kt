package com.example.spamscan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM cached_calls ORDER BY timestamp DESC")
    fun getAllCachedCalls(): Flow<List<CachedCall>>

    @Query("SELECT * FROM cached_calls WHERE id = :callId")
    suspend fun getCachedCallById(callId: Long): CachedCall?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CachedCall)

    @Query("DELETE FROM cached_calls")
    suspend fun clearCache()
}
