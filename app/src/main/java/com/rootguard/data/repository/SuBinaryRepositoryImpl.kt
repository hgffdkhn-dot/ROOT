package com.rootguard.data.repository

import android.content.Context
import android.os.Build
import com.rootguard.domain.model.SuBinary
import com.rootguard.domain.repository.SuBinaryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuBinaryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SuBinaryRepository {

    private val _suBinariesFlow = MutableStateFlow<List<SuBinary>>(emptyList())

    private val possibleFakeSuPaths = listOf(
        "/system/xbin/su",
        "/system/bin/su",
        "/vendor/bin/su",
        "/sbin/su"
    )

    override suspend fun getSystemSuPath(): String? = withContext(Dispatchers.IO) {
        possibleFakeSuPaths.firstOrNull { File(it).exists() }
    }

    override suspend fun getAvailableSuBinaries(): List<SuBinary> = withContext(Dispatchers.IO) {
        val binaries = mutableListOf<SuBinary>()

        possibleFakeSuPaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                binaries.add(
                    SuBinary(
                        name = file.name,
                        path = path,
                        arch = getArchitecture(file),
                        size = file.length(),
                        md5 = calculateMd5(file),
                        isFake = isFakeSu(file),
                        permissions = getPermissions(file)
                    )
                )
            }
        }

        val localSu = File(context.filesDir, "su")
        if (localSu.exists()) {
            binaries.add(
                SuBinary(
                    name = "su",
                    path = localSu.absolutePath,
                    arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                    size = localSu.length(),
                    md5 = calculateMd5(localSu),
                    isFake = false,
                    permissions = getPermissions(localSu)
                )
            )
        }

        _suBinariesFlow.value = binaries
        binaries
    }

    override suspend fun installFakeSu(targetPath: String): Result<SuBinary> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(targetPath)
            if (!targetFile.parentFile?.canWrite()!!) {
                return@withContext Result.failure(Exception("Cannot write to target path"))
            }

            val fakeSu = createFakeSu()
            FileOutputStream(targetFile).use { output ->
                fakeSu.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            targetFile.setExecutable(true, false)
            targetFile.setReadable(true, false)

            val binary = SuBinary(
                name = targetFile.name,
                path = targetPath,
                arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                size = targetFile.length(),
                md5 = calculateMd5(targetFile),
                isFake = true,
                permissions = getPermissions(targetFile)
            )

            Result.success(binary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createFakeSu(): ByteArray {
        return """
            #!/system/bin/sh
            # Fake SU binary - redirects to real manager
            am start -n com.rootguard/.presentation.MainActivity \
                -a android.intent.action.VIEW \
                -d "rootguard://auth?caller=${'$'}1&cmd=${'$'}2"
            exit 1
        """.toByteArray()
    }

    override suspend fun switchToRealSu(fakePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fakeFile = File(fakePath)
            val realSu = File(context.filesDir, "su")

            if (!realSu.exists()) {
                return@withContext Result.failure(Exception("Real SU not installed"))
            }

            val backupPath = fakePath + ".backup"
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                fakeFile.copyTo(backupFile)
            }

            realSu.inputStream().use { input ->
                FileOutputStream(fakeFile).use { output ->
                    input.copyTo(output)
                }
            }

            fakeFile.setExecutable(true, false)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifySuIntegrity(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || !file.canExecute()) {
                return@withContext false
            }

            val knownGoodMd5 = getKnownGoodMd5()
            if (knownGoodMd5 != null) {
                val currentMd5 = calculateMd5(file)
                return@withContext currentMd5 == knownGoodMd5
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getSuVersion(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            val process = Runtime.getRuntime().exec(arrayOf(path, "-v"))
            val reader = process.inputStream.bufferedReader()
            val version = reader.readLine()
            reader.close()
            process.destroy()

            version
        } catch (e: Exception) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf(path, "-V"))
                val reader = process.inputStream.bufferedReader()
                val version = reader.readLine()
                reader.close()
                process.destroy()
                version
            } catch (e2: Exception) {
                null
            }
        }
    }

    override fun observeSuBinaries(): Flow<List<SuBinary>> = _suBinariesFlow

    private fun isFakeSu(file: File): Boolean {
        return try {
            val content = file.readBytes().take(100).toString()
            content.contains("rootguard") || content.contains("fake")
        } catch (e: Exception) {
            false
        }
    }

    private fun getArchitecture(file: File): String {
        return try {
            val bytes = file.readBytes().take(20)
            when {
                bytes.any { it.toInt() and 0xFF > 127 } -> "arm64"
                else -> "arm"
            }
        } catch (e: Exception) {
            Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        }
    }

    private fun getPermissions(file: File): Int {
        var perms = 0
        if (file.canRead()) perms += 4
        if (file.canWrite()) perms += 2
        if (file.canExecute()) perms += 1
        return perms
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

    private fun getKnownGoodMd5(): String? {
        return context.getSharedPreferences("su_config", Context.MODE_PRIVATE)
            .getString("known_md5", null)
    }
}
