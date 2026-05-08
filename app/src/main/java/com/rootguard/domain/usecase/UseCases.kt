package com.rootguard.domain.usecase

import com.rootguard.domain.model.AppInfo
import com.rootguard.domain.model.RootStatus
import com.rootguard.domain.model.SecurityLog
import com.rootguard.domain.model.Settings
import com.rootguard.domain.repository.AppRepository
import com.rootguard.domain.repository.LogRepository
import com.rootguard.domain.repository.RootRepository
import com.rootguard.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckRootStatusUseCase @Inject constructor(
    private val rootRepository: RootRepository
) {
    suspend operator fun invoke(): RootStatus = rootRepository.checkRootStatus()
}

class GetInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): List<AppInfo> = appRepository.getInstalledApps()
}

class GetAppsWithRootUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): List<AppInfo> = appRepository.getAppsWithRoot()
    fun observe() = appRepository.observeRootApps()
}

class GrantRootUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val logRepository: LogRepository
) {
    suspend operator fun invoke(packageName: String, appName: String): Boolean {
        val result = appRepository.grantRoot(packageName)
        if (result) {
            logRepository.addLog(
                SecurityLog(
                    id = System.currentTimeMillis(),
                    packageName = packageName,
                    appName = appName,
                    action = com.rootguard.domain.model.LogAction.GRANT_ROOT,
                    timestamp = System.currentTimeMillis(),
                    details = "Root权限已授予"
                )
            )
        }
        return result
    }
}

class RevokeRootUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val logRepository: LogRepository
) {
    suspend operator fun invoke(packageName: String, appName: String): Boolean {
        val result = appRepository.revokeRoot(packageName)
        if (result) {
            logRepository.addLog(
                SecurityLog(
                    id = System.currentTimeMillis(),
                    packageName = packageName,
                    appName = appName,
                    action = com.rootguard.domain.model.LogAction.REVOKE_ROOT,
                    timestamp = System.currentTimeMillis(),
                    details = "Root权限已撤销"
                )
            )
        }
        return result
    }
}

class GetSecurityLogsUseCase @Inject constructor(
    private val logRepository: LogRepository
) {
    suspend operator fun invoke(): List<SecurityLog> = logRepository.getLogs()
    fun observe() = logRepository.observeLogs()
}

class ClearLogsUseCase @Inject constructor(
    private val logRepository: LogRepository
) {
    suspend operator fun invoke() = logRepository.clearLogs()
}

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Settings = settingsRepository.getSettings()
    fun observe() = settingsRepository.observeSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings) = settingsRepository.updateSettings(settings)
}
