package com.example.spamscan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.spamscan.data.AppPreferences
import com.example.spamscan.ml.SpamDetector

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
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""

                // Ensure ML model is loaded
                SpamDetector.initialize(context)

                // Run inference
                val result = SpamDetector.classify(context, body, currentThreshold)
                Log.d("SmsReceiver", "Received SMS from \$sender - Spam Prob: \${result.probability}")

                if (result.probability >= currentThreshold) {
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
