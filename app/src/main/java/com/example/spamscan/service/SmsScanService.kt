package com.example.spamscan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.spamscan.MainActivity
import com.example.spamscan.R
import com.example.spamscan.receiver.SmsReceiver

class SmsScanService : LifecycleService() {

    private val smsReceiver = SmsReceiver()
    private var isReceiverRegistered = false

    companion object {
        private const val CHANNEL_ID = "SpamScanServiceChannel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        registerSmsReceiver()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSmsReceiver()
    }

    private fun registerSmsReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            filter.priority = 999
            registerReceiver(smsReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterSmsReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(smsReceiver)
            isReceiverRegistered = false
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpamScan Is Active")
            .setContentText("Monitoring incoming SMS messages for spam")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon until UI assets added
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spam Scan Active Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that the SpamScan background service is running"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
