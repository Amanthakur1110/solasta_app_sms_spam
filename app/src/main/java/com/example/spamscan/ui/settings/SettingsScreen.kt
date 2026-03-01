package com.example.spamscan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spamscan.data.AppPreferences
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val currentThreshold by preferences.spamThreshold.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Subtle Grey Background
    ) {
        // Monochrome App Bar
        Surface(
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 48.dp, 16.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color(0xFFF1F3F4), CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "CONFIGURATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Model Sensitivity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Adjust the spam threshold. A lower threshold is more aggressive but might flag safe messages as spam.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Slider Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MILD", color = Color.LightGray, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text(
                            text = "${(currentThreshold * 100).roundToInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text("STRICT", color = Color.LightGray, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                    
                    Slider(
                        value = currentThreshold,
                        onValueChange = { newValue ->
                            preferences.setSpamThreshold(newValue)
                        },
                        valueRange = 0.05f..0.95f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color(0xFFEEEEEE)
                        ),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Watermark text
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Created by\nTEAM SYNTAX ERROR",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
