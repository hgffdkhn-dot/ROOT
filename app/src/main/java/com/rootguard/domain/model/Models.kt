package com.rootguard.domain.model

data class RootStatus(
    val isRooted: Boolean,
    val rootType: RootType,
    val lastCheckTime: Long,
    val suVersion: String? = null,
    val rootManager: String? = null
)

enum class RootType {
    NONE,
    FULL_ROOT,
    PARTIAL_ROOT,
    MAGISK,
    SUPERUSER
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val hasRootAccess: Boolean,
    val requestedAt: Long,
    val icon: android.graphics.drawable.Drawable? = null
)

data class SecurityLog(
    val id: Long,
    val packageName: String,
    val appName: String,
    val action: LogAction,
    val timestamp: Long,
    val details: String? = null
)

enum class LogAction {
    GRANT_ROOT,
    REVOKE_ROOT,
    REQUEST_ROOT,
    ROOT_DETECTED,
    ROOT_REMOVED
}

data class Settings(
    val autoMonitor: Boolean = true,
    val notifyOnRequest: Boolean = true,
    val darkMode: Boolean = false
)
