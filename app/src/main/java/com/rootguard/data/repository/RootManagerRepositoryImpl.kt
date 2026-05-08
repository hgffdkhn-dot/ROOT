package com.rootguard.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.rootguard.domain.model.*
import com.rootguard.domain.repository.RootManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RootManagerRepository {

    private val _rootStatusFlow = MutableStateFlow(
        RootStatus(false, false, false, null, RootMethod.NONE, System.currentTimeMillis())
    )

    private val suPaths = listOf(
        "/system/xbin/su",
        "/system/bin/su",
        "/vendor/bin/su",
        "/sbin/su",
        "/system/bin/.ext/su",
        "/system/usr/we-need-root/su"
    )

    override suspend fun checkRootStatus(): RootStatus = withContext(Dispatchers.IO) {
        val isRooted = suPaths.any { File(it).exists() }
        val hasMagisk = checkMagisk()
        val hasCustomSu = checkCustomSu()
        val suVersion = getSystemSuVersion()

        val method = when {
            hasMagisk -> RootMethod.MAGISK
            hasCustomSu && suPaths.any { it.contains("phh") } -> RootMethod.PHHMSU
            hasCustomSu -> RootMethod.CUSTOM_SU
            File("/sys/kernel/config").exists() -> RootMethod.KERNELSU
            else -> RootMethod.NONE
        }

        val status = RootStatus(
            isRooted = isRooted,
            hasMagisk = hasMagisk,
            hasCustomSu = hasCustomSu,
            suVersion = suVersion,
            rootMethod = method,
            lastCheckTime = System.currentTimeMillis()
        )

        _rootStatusFlow.value = status
        status
    }

    private fun checkMagisk(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.topjohnwu.magisk", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            File("/sbin/magisk").exists() ||
            File("/data/adb/magisk").exists() ||
            File("/system/bin/magiskinit").exists()
        }
    }

    private fun checkCustomSu(): Boolean {
        return suPaths.any { path ->
            File(path).let { it.exists() && it.canExecute() }
        }
    }

    private fun getSystemSuVersion(): String? {
        return try {
            suPaths.firstOrNull { File(it).exists() }?.let { path ->
                execCommand(arrayOf(path, "-V"))?.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun installSuBinary(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val assetsSu = extractSuFromAssets()
            if (assetsSu == null) {
                copySystemSu()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractSuFromAssets(): Boolean {
        return try {
            val suFile = File(context.filesDir, "su")
            context.assets.open("su").use { input ->
                suFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            suFile.setExecutable(true, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun copySystemSu() {
        val systemSu = suPaths.firstOrNull { File(it).exists() }
        if (systemSu != null) {
            val targetSu = File(context.filesDir, "su")
            File(systemSu).inputStream().use { input ->
                targetSu.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetSu.setExecutable(true, false)
        }
    }

    override suspend fun uninstallSuBinary(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val suFile = File(context.filesDir, "su")
            if (suFile.exists()) {
                suFile.delete()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSuBinaries(): List<SuBinary> = withContext(Dispatchers.IO) {
        val binaries = mutableListOf<SuBinary>()
        val filesDir = context.filesDir

        File(filesDir, "su").takeIf { it.exists() }?.let {
            binaries.add(
                SuBinary(
                    name = "su",
                    path = it.absolutePath,
                    arch = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                    size = it.length(),
                    md5 = calculateMd5(it),
                    isFake = false,
                    permissions = it.permissions
                )
            )
        }

        binaries
    }

    override suspend fun verifySuBinary(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.exists() && file.canExecute() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRootConfig(): RootConfig = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("root_config", Context.MODE_PRIVATE)
        RootConfig(
            isInstalled = prefs.getBoolean("is_installed", false),
            suPath = prefs.getString("su_path", "") ?: "",
            suVersion = prefs.getString("su_version", "") ?: "",
            suBinaryPath = prefs.getString("su_binary_path", "") ?: "",
            daemonPath = prefs.getString("daemon_path", "") ?: "",
            installationDate = prefs.getLong("install_date", 0L),
            lastUpdate = prefs.getLong("last_update", 0L)
        )
    }

    override suspend fun saveRootConfig(config: RootConfig) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("root_config", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_installed", config.isInstalled)
            putString("su_path", config.suPath)
            putString("su_version", config.suVersion)
            putString("su_binary_path", config.suBinaryPath)
            putString("daemon_path", config.daemonPath)
            putLong("install_date", config.installationDate)
            putLong("last_update", config.lastUpdate)
            apply()
        }
    }

    override fun observeRootStatus(): Flow<RootStatus> = _rootStatusFlow

    private fun execCommand(cmd: Array<String>): String? {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()
            process.destroy()
            output
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateMd5(file: File): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private val File.permissions: Int
        get() {
            var perms = 0
            if (canRead()) perms += 4
            if (canWrite()) perms += 2
            if (canExecute()) perms += 1
            return perms
        }
}
