package com.rootguard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rootguard.presentation.screens.AppsScreen
import com.rootguard.presentation.screens.BootManagerScreen
import com.rootguard.presentation.screens.HomeScreen
import com.rootguard.presentation.screens.LogsScreen
import com.rootguard.presentation.screens.SettingsScreen
import com.rootguard.presentation.screens.WhiteListScreen

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Apps.route) {
            AppsScreen()
        }
        composable(Screen.WhiteList.route) {
            WhiteListScreen()
        }
        composable(Screen.Logs.route) {
            LogsScreen()
        }
        composable(Screen.BootManager.route) {
            BootManagerScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
