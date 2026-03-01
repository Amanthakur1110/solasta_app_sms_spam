package com.example.spamscan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class OverlayType {
    SMS, CALL, POST_CALL
}

@Composable
fun SpamAlertOverlay(
    sender: String,
    body: String = "",
    probability: Float = 0f,
    type: OverlayType = OverlayType.SMS,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onBlock: ((String) -> Unit)? = null
) {
    // Auto dismiss after 10 seconds for alerts, maybe longer for post-call
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(if (type == OverlayType.POST_CALL) 15000 else 10000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconColor = when (type) {
                        OverlayType.SMS, OverlayType.CALL -> Color(0xFFE53935)
                        OverlayType.POST_CALL -> Color.Black
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(iconColor.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (type) {
                                OverlayType.SMS -> Icons.Filled.Warning
                                OverlayType.CALL -> Icons.Filled.Block
                                OverlayType.POST_CALL -> Icons.Filled.Warning
                            },
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (type) {
                                OverlayType.SMS -> "SPAM INTERCEPTED"
                                OverlayType.CALL -> "SPAM CALL DETECTED"
                                OverlayType.POST_CALL -> "UNIDENTIFIED CALL"
                            },
                            color = iconColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = sender,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (type == OverlayType.SMS) {
                            Text(
                                text = body,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (type == OverlayType.POST_CALL) {
                            Text(
                                text = "Mark this number as spam?",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Block, // Representing "Close" in a stylized way
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (type == OverlayType.POST_CALL || type == OverlayType.CALL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ThumbUp, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp), 
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ignore", color = Color.Gray, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { 
                                onBlock?.invoke(sender)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp), 
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spam", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
