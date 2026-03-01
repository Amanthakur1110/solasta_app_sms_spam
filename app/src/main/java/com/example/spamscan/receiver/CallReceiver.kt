package com.example.spamscan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.spamscan.data.local.AppDatabase
import com.example.spamscan.data.local.BlockedSender
import com.example.spamscan.data.local.CachedCall
import com.example.spamscan.service.SmsScanService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var lastIncomingNumber: String? = null
        private const val TAG = "CallReceiver"
        
        const val EXTRA_CALL_NUMBER = "extra_call_number"
        const val ACTION_SHOW_CALL_OVERLAY = "com.example.spamscan.ACTION_SHOW_CALL_OVERLAY"
        const val ACTION_SHOW_POST_CALL_REPORT = "com.example.spamscan.ACTION_SHOW_POST_CALL_REPORT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateString = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state = when (stateString) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> TelephonyManager.CALL_STATE_IDLE
        }

        handleStateChange(context, state, incomingNumber)
    }

    private fun handleStateChange(context: Context, state: Int, number: String?) {
        if (number != null) {
            lastIncomingNumber = number
        }

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "Ringing: $lastIncomingNumber")
                if (lastIncomingNumber != null) {
                    val number = lastIncomingNumber!!
                    val db = AppDatabase.getDatabase(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        val isBlocked = db.blockedSenderDao().isBlocked(number)
                        db.callDao().insertCall(CachedCall(sender = number, timestamp = System.currentTimeMillis(), isSpam = isBlocked))
                    }
                    checkAndShowIncomingAlert(context, number)
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "Offhook (Call Active)")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Idle (Call Ended/Missed)")
                if (lastState != TelephonyManager.CALL_STATE_IDLE && lastIncomingNumber != null) {
                    // Just transitioned to IDLE from RINGING or OFFHOOK
                    triggerPostCallReport(context, lastIncomingNumber!!)
                }
            }
        }
        lastState = state
    }

    private fun checkAndShowIncomingAlert(context: Context, number: String) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            if (db.blockedSenderDao().isBlocked(number)) {
                val intent = Intent(context, SmsScanService::class.java).apply {
                    action = ACTION_SHOW_CALL_OVERLAY
                    putExtra(EXTRA_CALL_NUMBER, number)
                }
                context.startForegroundService(intent)
            }
        }
    }

    private fun triggerPostCallReport(context: Context, number: String) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            // Only show report if the number isn't already blocked
            if (!db.blockedSenderDao().isBlocked(number)) {
                val intent = Intent(context, SmsScanService::class.java).apply {
                    action = ACTION_SHOW_POST_CALL_REPORT
                    putExtra(EXTRA_CALL_NUMBER, number)
                }
                context.startForegroundService(intent)
            }
        }
    }
}
