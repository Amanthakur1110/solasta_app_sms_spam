package com.example.spamscan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.spamscan.data.AppPreferences
import com.example.spamscan.ml.SpamDetector
import com.example.spamscan.data.local.AppDatabase
import com.example.spamscan.data.local.CachedSms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SPAM_DETECTED = "com.example.spamscan.SPAM_DETECTED"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_PROBABILITY = "extra_probability"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val preferences = AppPreferences(context)
            val currentThreshold = preferences.spamThreshold.value
            val useCustomModel = preferences.useCustomModel.value
            val db = AppDatabase.getDatabase(context)
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""

                // Run inference
                val result = SpamDetector.classify(context, body, currentThreshold, useCustomModel)
                Log.d("SmsReceiver", "Received SMS from $sender - Spam Prob: ${result.probability}")

                // Persist to database to trigger real-time refresh in Dashboard
                CoroutineScope(Dispatchers.IO).launch {
                    val timestamp = System.currentTimeMillis()
                    val cachedSms = CachedSms(
                        id = timestamp + body.hashCode(), // Simple unique ID
                        sender = sender,
                        body = body,
                        timestamp = timestamp,
                        spamProbability = result.probability,
                        isSpam = result.isSpam
                    )
                    db.smsDao().insertSms(cachedSms)
                }

                if (result.isSpam) {
                    val overlayIntent = Intent(ACTION_SPAM_DETECTED).apply {
                        putExtra(EXTRA_SENDER, sender)
                        putExtra(EXTRA_BODY, body)
                        putExtra(EXTRA_PROBABILITY, result.probability)
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(overlayIntent)
                }
            }
        }
    }
}
