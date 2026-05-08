package com.rootguard.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Apps : Screen("apps", "应用管理", Icons.Default.Apps)
    object WhiteList : Screen("whitelist", "白名单", Icons.Default.Security)
    object Logs : Screen("logs", "日志", Icons.Default.History)
    object BootManager : Screen("boot_manager", "Boot管理", Icons.Default.Build)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Apps,
    Screen.WhiteList,
    Screen.Logs,
    Screen.Settings
)
