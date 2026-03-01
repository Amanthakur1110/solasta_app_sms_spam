package com.example.spamscan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_calls")
data class CachedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val timestamp: Long,
    val isSpam: Boolean
)
