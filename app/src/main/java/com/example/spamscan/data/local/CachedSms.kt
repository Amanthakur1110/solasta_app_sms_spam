package com.example.spamscan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_sms")
data class CachedSms(
    @PrimaryKey val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val spamProbability: Float,
    val isSpam: Boolean
)
