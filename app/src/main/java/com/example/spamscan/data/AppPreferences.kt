package com.example.spamscan.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spamscan_prefs", Context.MODE_PRIVATE)

    private val _isServiceRunning = MutableStateFlow(prefs.getBoolean("service_running", false))
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _spamThreshold = MutableStateFlow(prefs.getFloat("spam_threshold", 0.5f))
    val spamThreshold: StateFlow<Float> = _spamThreshold.asStateFlow()

    fun setServiceRunning(isRunning: Boolean) {
        prefs.edit().putBoolean("service_running", isRunning).apply()
        _isServiceRunning.value = isRunning
    }

    fun setSpamThreshold(threshold: Float) {
        prefs.edit().putFloat("spam_threshold", threshold).apply()
        _spamThreshold.value = threshold
    }
}
