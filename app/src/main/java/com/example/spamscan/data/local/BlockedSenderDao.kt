package com.example.spamscan.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSenderDao {
    @Query("SELECT * FROM blocked_senders")
    fun getAllBlockedSenders(): Flow<List<BlockedSender>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_senders WHERE phoneNumber = :phoneNumber)")
    suspend fun isBlocked(phoneNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockSender(blockedSender: BlockedSender)

    @Delete
    suspend fun unblockSender(blockedSender: BlockedSender)

    @Query("DELETE FROM blocked_senders WHERE phoneNumber = :phoneNumber")
    suspend fun unblockSender(phoneNumber: String)
}
