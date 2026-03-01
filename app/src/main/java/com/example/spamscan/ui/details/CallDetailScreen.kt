package com.example.spamscan.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spamscan.data.local.AppDatabase
import com.example.spamscan.data.local.BlockedSender
import com.example.spamscan.data.local.CachedCall
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var call by remember { mutableStateOf<CachedCall?>(null) }
    var isBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(callId) {
        call = db.callDao().getCachedCallById(callId)
        call?.let {
            isBlocked = db.blockedSenderDao().isBlocked(it.sender)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CALL DETAILS", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            call?.let { item ->
                // Status Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(if (item.isSpam) Color.Black else Color.White, CircleShape)
                                .border(1.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.isSpam) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (item.isSpam) Color.White else Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (item.isSpam) "SPAM CALL DETECTED" else "SAFE CALL",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Number Detail
                DetailRow(label = "PHONE NUMBER", value = item.sender)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.Black.copy(alpha = 0.1f))
                
                // Date Detail
                val date = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                DetailRow(label = "CALL TIME", value = date)

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                Button(
                    onClick = {
                        scope.launch {
                            if (isBlocked) {
                                db.blockedSenderDao().unblockSender(item.sender)
                            } else {
                                db.blockedSenderDao().blockSender(BlockedSender(item.sender))
                            }
                            isBlocked = !isBlocked
                            // Update the call record as well? 
                            // Actually, isSpam in CachedCall is just a snapshot. 
                            // But we could refresh it or just rely on BlockedSender table.
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) Color.White else Color.Black,
                        contentColor = if (isBlocked) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isBlocked) "UNBLOCK NUMBER" else "BLOCK NUMBER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
