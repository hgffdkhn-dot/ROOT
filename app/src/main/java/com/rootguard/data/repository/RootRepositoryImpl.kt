package com.rootguard.data.repository

import com.rootguard.domain.model.RootStatus
import com.rootguard.domain.model.RootType
import com.rootguard.domain.repository.RootRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootRepositoryImpl @Inject constructor() : RootRepository {

    override suspend fun checkRootStatus(): RootStatus {
        return try {
            val isRooted = checkRootFiles() || checkRootCommands()
            val rootType = detectRootType()
            val suVersion = getSuVersion()

            RootStatus(
                isRooted = isRooted,
                rootType = rootType,
                lastCheckTime = System.currentTimeMillis(),
                suVersion = suVersion,
                rootManager = if (isRooted) "RootGuard" else null
            )
        } catch (e: Exception) {
            RootStatus(
                isRooted = false,
                rootType = RootType.NONE,
                lastCheckTime = System.currentTimeMillis()
            )
        }
    }

    override fun observeRootStatus(): Flow<RootStatus> = flow {
        while (true) {
            emit(checkRootStatus())
            delay(5000)
        }
    }

    private fun checkRootFiles(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return rootPaths.any { File(it).exists() }
    }

    private fun checkRootCommands(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            val result = reader.readLine()
            reader.close()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    private fun detectRootType(): RootType {
        return when {
            File("/sbin/su").exists() || File("/system/xbin/su").exists() -> RootType.MAGISK
            File("/system/app/Superuser.apk").exists() -> RootType.SUPERUSER
            checkRootFiles() -> RootType.FULL_ROOT
            else -> RootType.NONE
        }
    }

    private fun getSuVersion(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/su", "-v"))
            val reader = process.inputStream.bufferedReader()
            val version = reader.readLine()
            reader.close()
            version
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
