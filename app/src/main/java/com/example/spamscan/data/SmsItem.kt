package com.example.spamscan.data

data class SmsItem(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val spamProbability: Float,
    val isSpam: Boolean
)
