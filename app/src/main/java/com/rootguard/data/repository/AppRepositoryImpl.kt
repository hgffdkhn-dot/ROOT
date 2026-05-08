package com.rootguard.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.rootguard.domain.model.AppInfo
import com.rootguard.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    private val rootAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private val simulatedRootApps = mutableSetOf(
        "com.android.settings",
        "com.termux",
        "com.topjohnwu.magisk"
    )

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
            app.packageName == "com.android.settings"
        }.mapNotNull { app ->
            try {
                AppInfo(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    hasRootAccess = simulatedRootApps.contains(app.packageName),
                    requestedAt = System.currentTimeMillis(),
                    icon = pm.getApplicationIcon(app)
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    override suspend fun getAppsWithRoot(): List<AppInfo> {
        return getInstalledApps().filter { it.hasRootAccess }
    }

    override suspend fun hasRootAccess(packageName: String): Boolean {
        return simulatedRootApps.contains(packageName)
    }

    override suspend fun grantRoot(packageName: String): Boolean {
        return try {
            simulatedRootApps.add(packageName)
            refreshRootAppsFlow()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun revokeRoot(packageName: String): Boolean {
        return try {
            simulatedRootApps.remove(packageName)
            refreshRootAppsFlow()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun observeRootApps(): Flow<List<AppInfo>> = rootAppsFlow.asStateFlow()

    private suspend fun refreshRootAppsFlow() {
        rootAppsFlow.value = getAppsWithRoot()
    }
}
