package com.rootguard.domain.repository

import com.rootguard.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RootManagerRepository {
    suspend fun checkRootStatus(): RootStatus
    suspend fun installSuBinary(): Result<Boolean>
    suspend fun uninstallSuBinary(): Result<Boolean>
    suspend fun getSuBinaries(): List<SuBinary>
    suspend fun verifySuBinary(path: String): Boolean
    suspend fun getRootConfig(): RootConfig
    suspend fun saveRootConfig(config: RootConfig)
    fun observeRootStatus(): Flow<RootStatus>
}

interface WhiteListRepository {
    suspend fun getWhiteList(): List<WhiteListEntry>
    suspend fun addToWhiteList(entry: WhiteListEntry): Result<Unit>
    suspend fun removeFromWhiteList(packageName: String): Result<Unit>
    suspend fun updateWhiteListEntry(entry: WhiteListEntry): Result<Unit>
    suspend fun isInWhiteList(packageName: String): Boolean
    suspend fun getWhiteListEntry(packageName: String): WhiteListEntry?
    fun observeWhiteList(): Flow<List<WhiteListEntry>>
}

interface AuthRequestRepository {
    suspend fun createAuthRequest(
        packageName: String,
        appName: String,
        command: String?,
        path: String?,
        uid: Int,
        pid: Int
    ): AuthRequest

    suspend fun getPendingRequests(): List<AuthRequest>
    suspend fun getRequestHistory(): List<AuthRequest>
    suspend fun approveRequest(requestId: String): Result<Unit>
    suspend fun denyRequest(requestId: String): Result<Unit>
    suspend fun getRequest(requestId: String): AuthRequest?
    suspend fun clearHistory(): Result<Unit>
    fun observeAuthRequests(): Flow<List<AuthRequest>>
}

interface SuBinaryRepository {
    suspend fun getSystemSuPath(): String?
    suspend fun getAvailableSuBinaries(): List<SuBinary>
    suspend fun installFakeSu(targetPath: String): Result<SuBinary>
    suspend fun switchToRealSu(fakePath: String): Result<Unit>
    suspend fun verifySuIntegrity(path: String): Boolean
    suspend fun getSuVersion(path: String): String?
    fun observeSuBinaries(): Flow<List<SuBinary>>
}

interface BootManagerRepository {
    suspend fun patchBootImage(): Result<ByteArray>
    suspend fun injectSuToBoot(bootImage: ByteArray): Result<ByteArray>
    suspend fun verifyBootPatch(): Boolean
    suspend fun getBootStage(): BootStage?
    suspend fun saveBootStage(stage: BootStage)
    suspend fun backupBootImage(): Result<ByteArray>
    suspend fun restoreBootImage(backup: ByteArray): Result<Unit>
    fun observeBootStage(): Flow<BootStage>
}
