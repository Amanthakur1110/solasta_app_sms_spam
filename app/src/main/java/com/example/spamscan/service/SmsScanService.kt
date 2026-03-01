package com.example.spamscan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.spamscan.MainActivity
import com.example.spamscan.R
import com.example.spamscan.receiver.SmsReceiver
import com.example.spamscan.ui.components.SpamAlertOverlay
import com.example.spamscan.ui.theme.SpamscanTheme

class SmsScanService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    private val smsReceiver = SmsReceiver()
    private var isReceiverRegistered = false
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val globalSpamReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_SPAM_DETECTED) {
                val sender = intent.getStringExtra(SmsReceiver.EXTRA_SENDER) ?: ""
                val body = intent.getStringExtra(SmsReceiver.EXTRA_BODY) ?: ""
                val prob = intent.getFloatExtra(SmsReceiver.EXTRA_PROBABILITY, 0f)
                showGlobalOverlay(sender, body, prob)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "SpamScanServiceChannel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            globalSpamReceiver,
            IntentFilter(SmsReceiver.ACTION_SPAM_DETECTED)
        )
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(globalSpamReceiver)
        removeOverlay()
        _viewModelStore.clear()
    }

    private fun showGlobalOverlay(sender: String, body: String, probability: Float) {
        removeOverlay() // Ensure previous one is gone

        val hostView = ComposeView(this).apply {
            // Lifecycle and SavedState boilerplate for Compose in Service
            setViewTreeLifecycleOwner(this@SmsScanService)
            setViewTreeViewModelStoreOwner(this@SmsScanService)
            setViewTreeSavedStateRegistryOwner(this@SmsScanService)
            
            setContent {
                var isVisible by remember { mutableStateOf(true) }
                SpamscanTheme {
                    SpamAlertOverlay(
                        sender = sender,
                        body = body,
                        probability = probability,
                        isVisible = isVisible,
                        onDismiss = { 
                            isVisible = false
                            // Delay removal to allow animation
                            Handler(Looper.getMainLooper()).postDelayed({
                                removeOverlay()
                            }, 1000)
                        }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 50 // Inset from top
        }

        try {
            windowManager.addView(hostView, params)
            overlayView = hostView
            
            // Auto dismiss backup
            Handler(Looper.getMainLooper()).postDelayed({
                removeOverlay()
            }, 12000) // Slightly longer than the 10s internal timer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
        }
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
