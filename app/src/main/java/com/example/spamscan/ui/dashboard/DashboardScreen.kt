package com.example.spamscan.ui.dashboard

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spamscan.data.AppPreferences
import com.example.spamscan.data.SmsItem
import com.example.spamscan.ml.SmsInboxScanner
import com.example.spamscan.service.SmsScanService
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.example.spamscan.data.local.AppDatabase

@Composable
fun DashboardScreen(
    preferences: AppPreferences,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRunning by preferences.isServiceRunning.collectAsState()
    val spamThreshold by preferences.spamThreshold.collectAsState()

    // Observe Room database Flow for instant refresh
    val db = remember { AppDatabase.getDatabase(context) }
    val cachedMessages by db.smsDao().getAllCachedSms().collectAsState(initial = emptyList())

    // Map CachedSms to SmsItem for the UI
    val messages = remember(cachedMessages) {
        cachedMessages.map { 
            SmsItem(it.id, it.sender, it.body, it.timestamp, it.spamProbability, it.isSpam)
        }
    }

    var isScanning by remember { mutableStateOf(false) }
    var hasPermission by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.READ_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) 
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                isScanning = true
                val scanner = SmsInboxScanner(context)
                scanner.scanInbox()
                isScanning = false
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && cachedMessages.isEmpty()) {
            coroutineScope.launch {
                isScanning = true
                val scanner = SmsInboxScanner(context)
                scanner.scanInbox()
                isScanning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Subtle Grey Background
    ) {
        // Modern Monochrome Header
        Surface(
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 48.dp, 24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Column {
                    Text(
                        text = "Dashboard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SpamScan",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
                
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(Color(0xFFF1F3F4), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                StatusHeaderItem(isRunning = isRunning, preferences = preferences)
            }

            if (isScanning && messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp)
                    }
                }
            } else if (!hasPermission) {
                item {
                    PermissionRequestItem(onGrantClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) })
                }
            } else if (messages.isEmpty() && !isScanning) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No messages found.", color = Color.LightGray, fontSize = 16.sp)
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, bottom = 12.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inbox Analysis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (isScanning) {
                            Text("Updating...", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        MessageCard(msg, spamThreshold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusHeaderItem(isRunning: Boolean, preferences: AppPreferences) {
    val context = LocalContext.current
    val transition = updateTransition(isRunning, label = "ButtonState")
    val buttonColor by transition.animateColor(
        transitionSpec = { tween(800) },
        label = "ButtonColor"
    ) { state ->
        if (state) Color.Black else Color(0xFFE0E0E0)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated Monochrome Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val newRunningState = !isRunning
                    preferences.setServiceRunning(newRunningState)
                    val serviceIntent = Intent(context, SmsScanService::class.java)
                    if (newRunningState) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.stopService(serviceIntent)
                    }
                }
        ) {
            // Pulse Ring
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                )
            }

            // Main Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .shadow(if (isRunning) 10.dp else 0.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser, // Using safer default icons
                    contentDescription = "Toggle Service",
                    tint = if (isRunning) Color.White else Color.DarkGray,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isRunning) "SCANNING ACTIVE" else "SCANNER PAUSED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = if (isRunning) Color.Black else Color.Gray,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun PermissionRequestItem(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock, 
                contentDescription = null, 
                modifier = Modifier.size(48.dp), 
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permissions Required", 
                color = Color.Black, 
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Allow SMS access to enable real-time spam detection and inbox analysis.", 
                color = Color.Gray, 
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enable Protection")
            }
        }
    }
}

@Composable
fun MessageCard(msg: SmsItem, spamThreshold: Float) {
    val isCurrentlySpam = msg.spamProbability >= spamThreshold
    
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Monochrome Status Icon with Ticks
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF8F9FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentlySpam) {
                    Icon(
                        imageVector = Icons.Filled.Close, 
                        contentDescription = "Spam", 
                        tint = Color(0xFFE53935), 
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check, 
                        contentDescription = "Safe", 
                        tint = Color(0xFF43A047), 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.sender,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(msg.timestamp).toString(),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = msg.body,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCurrentlySpam) "SPAM" else "SAFE",
                        color = if (isCurrentlySpam) Color(0xFFE53935) else Color(0xFF43A047),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(msg.spamProbability * 100).toInt()}% match",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
