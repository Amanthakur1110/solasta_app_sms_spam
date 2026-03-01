package com.example.spamscan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_senders")
data class BlockedSender(
    @PrimaryKey val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)
