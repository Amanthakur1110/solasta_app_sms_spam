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
    object MessageDetail : AppScreen("detail/{smsId}") {
        fun createRoute(smsId: Long) = "detail/$smsId"
    }
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
                onNavigateToSettings = { navController.navigate(AppScreen.Settings.route) },
                onNavigateToDetail = { smsId -> 
                    navController.navigate(AppScreen.MessageDetail.createRoute(smsId))
                }
            )
        }
        composable(AppScreen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppScreen.MessageDetail.route,
            arguments = listOf(androidx.navigation.navArgument("smsId") { type = androidx.navigation.NavType.LongType })
        ) { backStackEntry ->
            val smsId = backStackEntry.arguments?.getLong("smsId") ?: 0L
            com.example.spamscan.ui.details.MessageDetailScreen(
                smsId = smsId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
