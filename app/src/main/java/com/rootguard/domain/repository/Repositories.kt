package com.rootguard.domain.repository

import com.rootguard.domain.model.AppInfo
import com.rootguard.domain.model.RootStatus
import com.rootguard.domain.model.SecurityLog
import com.rootguard.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface RootRepository {
    suspend fun checkRootStatus(): RootStatus
    fun observeRootStatus(): Flow<RootStatus>
}

interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getAppsWithRoot(): List<AppInfo>
    suspend fun hasRootAccess(packageName: String): Boolean
    suspend fun grantRoot(packageName: String): Boolean
    suspend fun revokeRoot(packageName: String): Boolean
    fun observeRootApps(): Flow<List<AppInfo>>
}

interface LogRepository {
    suspend fun getLogs(): List<SecurityLog>
    suspend fun addLog(log: SecurityLog)
    suspend fun clearLogs()
    fun observeLogs(): Flow<List<SecurityLog>>
}

interface SettingsRepository {
    suspend fun getSettings(): Settings
    suspend fun updateSettings(settings: Settings)
    fun observeSettings(): Flow<Settings>
}
