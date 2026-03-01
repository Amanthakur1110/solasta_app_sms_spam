package com.example.spamscan

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.spamscan.data.AppPreferences
import com.example.spamscan.receiver.SmsReceiver
import com.example.spamscan.ui.components.SpamAlertOverlay
import com.example.spamscan.ui.navigation.AppNavigation
import com.example.spamscan.ui.theme.SpamscanTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    // Overlay State
    private var isOverlayVisible by mutableStateOf(false)
    private var overlaySender by mutableStateOf("")
    private var overlayBody by mutableStateOf("")
    private var overlayProbability by mutableStateOf(0f)

    private val spamReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_SPAM_DETECTED) {
                overlaySender = intent.getStringExtra(SmsReceiver.EXTRA_SENDER) ?: ""
                overlayBody = intent.getStringExtra(SmsReceiver.EXTRA_BODY) ?: ""
                overlayProbability = intent.getFloatExtra(SmsReceiver.EXTRA_PROBABILITY, 0f)
                isOverlayVisible = true
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        if (!smsGranted) {
            Toast.makeText(this, "SMS Permissions required for app to function.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferences = AppPreferences(this)
        requestRequiredPermissions()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            spamReceiver,
            IntentFilter(SmsReceiver.ACTION_SPAM_DETECTED)
        )

        enableEdgeToEdge()
        setContent {
            SpamscanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        
                        AppNavigation(preferences)

                        // Float the overlay on top of everything
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            SpamAlertOverlay(
                                sender = overlaySender,
                                body = overlayBody,
                                probability = overlayProbability,
                                isVisible = isOverlayVisible,
                                onDismiss = { isOverlayVisible = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(spamReceiver)
    }
}