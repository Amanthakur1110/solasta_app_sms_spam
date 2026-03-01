package com.example.spamscan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spamscan.data.AppPreferences
import com.example.spamscan.ui.dashboard.DashboardScreen
import com.example.spamscan.ui.settings.SettingsScreen

sealed class AppScreen(val route: String) {
    object Dashboard : AppScreen("dashboard")
    object Settings : AppScreen("settings")
}

@Composable
fun AppNavigation(preferences: AppPreferences) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreen.Dashboard.route
    ) {
        composable(AppScreen.Dashboard.route) {
            DashboardScreen(
                preferences = preferences,
                onNavigateToSettings = { navController.navigate(AppScreen.Settings.route) }
            )
        }
        composable(AppScreen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
