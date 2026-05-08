package com.rootguard.data.repository

import android.content.Context
import com.rootguard.domain.model.BootStage
import com.rootguard.domain.model.Stage
import com.rootguard.domain.repository.BootManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BootManagerRepository {

    private val prefs = context.getSharedPreferences("boot_manager", Context.MODE_PRIVATE)
    private val _bootStageFlow = MutableStateFlow<BootStage?>(null)

    private val bootPaths = listOf(
        "/dev/block/by-name/boot",
        "/dev/block/bootdevice/by-name/boot",
        "/dev/block/platform/soc/7824900.sdhci/by-name/boot"
    )

    private val MAGIC_SIZE = 8
    private val KERNEL_SIZE_OFFSET = 8
    private val RAMDISK_SIZE_OFFSET = 16
    private val SECOND_SIZE_OFFSET = 24
    private val KERNEL_ADDR_OFFSET = 32
    private val RAMDISK_ADDR_OFFSET = 40
    private val SECOND_ADDR_OFFSET = 48
    private val TAGS_ADDR_OFFSET = 56
    private val PAGE_SIZE_OFFSET = 64
    private val DT_SIZE_OFFSET = 72
    private val OS_VERSION_OFFSET = 80
    private val OS_PATCH_LEVEL_OFFSET = 84
    private val HEADER_SIZE = 1024

    override suspend fun patchBootImage(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            saveBootStage(BootStage(Stage.DETECTING, false, "检测 boot 镜像...", System.currentTimeMillis()))

            val originalBoot = findAndReadBootImage()
                ?: return@withContext Result.failure(Exception("无法找到 boot 镜像"))

            saveBootStage(BootStage(Stage.PATCHING, false, "正在备份 boot 镜像...", System.currentTimeMillis()))

            val backupResult = backupBootImage()
            if (backupResult.isFailure) {
                return@withContext Result.failure(Exception("备份失败: ${backupResult.exceptionOrNull()?.message}"))
            }

            saveBootStage(BootStage(Stage.INJECTING_SU, false, "正在解析 boot 镜像...", System.currentTimeMillis()))

            val header = parseBootHeader(originalBoot)
            if (header == null) {
                return@withContext Result.failure(Exception("无法解析 boot 镜像头"))
            }

            saveBootStage(BootStage(Stage.INJECTING_SU, false, "正在提取 ramdisk...", System.currentTimeMillis()))

            val ramdiskData = extractRamdiskData(originalBoot, header)
            if (ramdiskData.isEmpty()) {
                return@withContext Result.failure(Exception("无法提取 ramdisk"))
            }

            saveBootStage(BootStage(Stage.INJECTING_SU, false, "正在修改 ramdisk...", System.currentTimeMillis()))

            val modifiedRamdisk = modifyRamdiskSafely(ramdiskData)
            if (modifiedRamdisk.isEmpty()) {
                return@withContext Result.failure(Exception("无法修改 ramdisk"))
            }

            saveBootStage(BootStage(Stage.INJECTING_SU, false, "正在重建 boot 镜像...", System.currentTimeMillis()))

            val patchedBoot = rebuildBootImage(originalBoot, header, modifiedRamdisk)
            if (patchedBoot.isEmpty()) {
                return@withContext Result.failure(Exception("无法重建 boot 镜像"))
            }

            saveBootStage(BootStage(Stage.VERIFYING, false, "正在验证修改...", System.currentTimeMillis()))

            if (!verifyPatchSafety(patchedBoot)) {
                restoreBootImage(originalBoot)
                return@withContext Result.failure(Exception("修改验证失败，已恢复原镜像"))
            }

            saveBootStage(BootStage(Stage.COMPLETED, true, "修改完成！请重启设备", System.currentTimeMillis()))

            Result.success(patchedBoot)
        } catch (e: Exception) {
            saveBootStage(BootStage(Stage.FAILED, false, "错误: ${e.message}", System.currentTimeMillis()))
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseBootHeader(data: ByteArray): BootHeader? {
        if (data.size < HEADER_SIZE) return null

        val magic = data.copyOfRange(0, MAGIC_SIZE).toString(Charsets.UTF_8).trim()
        if (!magic.startsWith("ANDROID!")) {
            return null
        }

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return BootHeader(
            magic = magic,
            kernelSize = buffer.getLong(KERNEL_SIZE_OFFSET),
            ramdiskSize = buffer.getLong(RAMDISK_SIZE_OFFSET),
            secondSize = buffer.getLong(SECOND_SIZE_OFFSET),
            kernelAddr = buffer.getLong(KERNEL_ADDR_OFFSET),
            ramdiskAddr = buffer.getLong(RAMDISK_ADDR_OFFSET),
            secondAddr = buffer.getLong(SECOND_ADDR_OFFSET),
            tagsAddr = buffer.getLong(TAGS_ADDR_OFFSET),
            pageSize = buffer.getInt(PAGE_SIZE_OFFSET).toLong(),
            dtSize = buffer.getLong(DT_SIZE_OFFSET),
            osVersion = buffer.getInt(OS_VERSION_OFFSET),
            osPatchLevel = buffer.getInt(OS_PATCH_LEVEL_OFFSET)
        )
    }

    private fun extractRamdiskData(bootImage: ByteArray, header: BootHeader): ByteArray {
        val kernelOffset = header.pageSize
        val ramdiskOffset = kernelOffset + alignSize(header.kernelSize, header.pageSize)
        return bootImage.copyOfRange(ramdiskOffset.toInt(), (ramdiskOffset + header.ramdiskSize).toInt())
    }

    private fun alignSize(size: Long, alignment: Long): Long {
        return if (size % alignment == 0L) size else ((size / alignment) + 1) * alignment
    }

    private fun modifyRamdiskSafely(ramdiskData: ByteArray): ByteArray {
        val tempDir = File(context.cacheDir, "ramdisk_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        return try {
            extractRamdiskToDirectory(ramdiskData, tempDir)
            injectSuFiles(tempDir)
            packDirectoryToRamdisk(tempDir)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun extractRamdiskToDirectory(ramdiskData: ByteArray, destDir: File) {
        val cpioFile = File(destDir, "ramdisk.cpio")
        FileOutputStream(cpioFile).use { it.write(ramdiskData) }

        val process = Runtime.getRuntime().exec(
            arrayOf("sh", "-c", "cd ${destDir.absolutePath} && cpio -id < ramdisk.cpio 2>/dev/null")
        )
        process.waitFor()
    }

    private fun injectSuFiles(rootfsDir: File) {
        val sbinDir = File(rootfsDir, "sbin")
        if (!sbinDir.exists()) sbinDir.mkdirs()

        val suFile = File(context.filesDir, "su")
        if (suFile.exists()) {
            val targetSu = File(sbinDir, "su")
            suFile.copyTo(targetSu, overwrite = true)
            targetSu.setExecutable(true, false)
            targetSu.setReadable(true, false)
            targetSu.setWritable(true, false)
        }

        createInitScript(rootfsDir)
    }

    private fun createInitScript(rootfsDir: File) {
        val initRc = File(rootfsDir, "init.rc")
        if (initRc.exists()) {
            val content = initRc.readText()
            if (!content.contains("rootguard")) {
                initRc.appendText(createSuServiceScript())
            }
        } else {
            initRc.writeText(createSuServiceScript())
        }
    }

    private fun createSuServiceScript(): String {
        return """
            # RootGuard SU Service - Safe Root Management
            service rootguardd /sbin/su --daemon
                class main
                user root
                group root
                oneshot
                disabled
        """.trimIndent()
    }

    private fun packDirectoryToRamdisk(rootfsDir: File): ByteArray {
        val cpioFile = File(rootfsDir, "ramdisk_new.cpio")

        val process = Runtime.getRuntime().exec(
            arrayOf("sh", "-c", "cd ${rootfsDir.absolutePath} && find . | cpio -o -H newc > ${cpioFile.absolutePath}")
        )
        process.waitFor()

        return if (cpioFile.exists()) {
            FileInputStream(cpioFile).use { it.readBytes() }
        } else {
            ByteArray(0)
        }
    }

    private fun rebuildBootImage(originalBoot: ByteArray, header: BootHeader, newRamdisk: ByteArray): ByteArray {
        try {
            val newSize = header.pageSize +
                    alignSize(header.kernelSize, header.pageSize) +
                    alignSize(newRamdisk.size.toLong(), header.pageSize) +
                    alignSize(header.secondSize, header.pageSize) +
                    alignSize(header.dtSize, header.pageSize)

            val newBoot = ByteArray(newSize.toInt())
            System.arraycopy(originalBoot, 0, newBoot, 0, originalBoot.size.coerceAtMost(newBoot.size))

            val ramdiskOffset = header.pageSize + alignSize(header.kernelSize, header.pageSize)
            System.arraycopy(newRamdisk, 0, newBoot, ramdiskOffset.toInt(), newRamdisk.size)

            val buffer = ByteBuffer.wrap(newBoot)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putLong(RAMDISK_SIZE_OFFSET, newRamdisk.size.toLong())

            return newBoot
        } catch (e: Exception) {
            e.printStackTrace()
            return ByteArray(0)
        }
    }

    private fun verifyPatchSafety(patchedBoot: ByteArray): Boolean {
        if (patchedBoot.size < HEADER_SIZE) return false

        val header = parseBootHeader(patchedBoot) ?: return false

        return header.magic.startsWith("ANDROID!") &&
               header.ramdiskSize > 0 &&
               header.kernelSize > 0 &&
               header.pageSize in 2048..65536
    }

    private fun findAndReadBootImage(): ByteArray? {
        for (path in bootPaths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    return file.readBytes()
                }
            } catch (e: Exception) {
                continue
            }
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("dd", "if=/dev/block/bootdevice/by-name/boot", "bs=4096"))
            process.inputStream.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun injectSuToBoot(bootImage: ByteArray): Result<ByteArray> {
        return patchBootImage()
    }

    override suspend fun verifyBootPatch(): Boolean = withContext(Dispatchers.IO) {
        val suBin = File("/sbin/su")
        suBin.exists() && suBin.canExecute()
    }

    override suspend fun getBootStage(): BootStage? = withContext(Dispatchers.IO) {
        _bootStageFlow.value
    }

    override suspend fun saveBootStage(stage: BootStage) = withContext(Dispatchers.IO) {
        _bootStageFlow.value = stage

        prefs.edit().apply {
            putString("stage", stage.stage.name)
            putBoolean("completed", stage.isCompleted)
            putString("message", stage.message)
            putLong("timestamp", stage.timestamp)
            apply()
        }
    }

    override suspend fun backupBootImage(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val bootImage = findAndReadBootImage()
                ?: return@withContext Result.failure(Exception("无法读取 boot 镜像"))

            val backupDir = File(context.filesDir, "backups")
            backupDir.mkdirs()

            val backupFile = File(backupDir, "boot_backup_${System.currentTimeMillis()}.img")
            FileOutputStream(backupFile).use { it.write(bootImage) }

            prefs.edit().putString("last_backup", backupFile.absolutePath).apply()

            Result.success(bootImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBootImage(backup: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bootPath = bootPaths.firstOrNull { File(it).canWrite() }
                ?: return@withContext Result.failure(Exception("无法找到可写入的 boot 分区"))

            Runtime.getRuntime().exec(arrayOf("dd", "if=/dev/stdin", "of=$bootPath", "bs=4096")).apply {
                outputStream.use { it.write(backup) }
                waitFor()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBootStage(): Flow<BootStage> = _bootStageFlow

    private data class BootHeader(
        val magic: String,
        val kernelSize: Long,
        val ramdiskSize: Long,
        val secondSize: Long,
        val kernelAddr: Long,
        val ramdiskAddr: Long,
        val secondAddr: Long,
        val tagsAddr: Long,
        val pageSize: Long,
        val dtSize: Long,
        val osVersion: Int,
        val osPatchLevel: Int
    )
}
