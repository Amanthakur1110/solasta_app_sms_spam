package com.example.spamscan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PrimaryIndigo = Color(0xFF5C6BC0)
val SoftWhite = Color(0xFFF8F9FF)
val SpamRed = Color(0xFFFF5252)
val SpamOrange = Color(0xFFFF8A65)
val SafeBlue = Color(0xFF42A5F5)
val SafeTeal = Color(0xFF26A69A)

// Enforcing Light Apple-like Theme per requirements
private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    secondary = SafeTeal,
    tertiary = SpamOrange,
    background = SoftWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun SpamscanTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Force light theme for Apple-like pristine look
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Using default typography for now, will refine
        content = content
    )
}