package com.rootguard.domain.usecase

import com.rootguard.domain.model.*
import com.rootguard.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckRootStatusUseCase @Inject constructor(
    private val rootManagerRepository: RootManagerRepository
) {
    suspend operator fun invoke(): RootStatus = rootManagerRepository.checkRootStatus()
    fun observe(): Flow<RootStatus> = rootManagerRepository.observeRootStatus()
}

class InstallSuBinaryUseCase @Inject constructor(
    private val rootManagerRepository: RootManagerRepository,
    private val suBinaryRepository: SuBinaryRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        val binaries = suBinaryRepository.getAvailableSuBinaries()
        if (binaries.isEmpty()) {
            return Result.failure(Exception("No SU binaries available"))
        }
        return rootManagerRepository.installSuBinary()
    }
}

class UninstallSuBinaryUseCase @Inject constructor(
    private val rootManagerRepository: RootManagerRepository
) {
    suspend operator fun invoke(): Result<Boolean> = rootManagerRepository.uninstallSuBinary()
}

class GetSuBinariesUseCase @Inject constructor(
    private val suBinaryRepository: SuBinaryRepository
) {
    suspend operator fun invoke(): List<SuBinary> = suBinaryRepository.getAvailableSuBinaries()
    fun observe(): Flow<List<SuBinary>> = suBinaryRepository.observeSuBinaries()
}

class GetWhiteListUseCase @Inject constructor(
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(): List<WhiteListEntry> = whiteListRepository.getWhiteList()
    fun observe(): Flow<List<WhiteListEntry>> = whiteListRepository.observeWhiteList()
}

class AddToWhiteListUseCase @Inject constructor(
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(entry: WhiteListEntry): Result<Unit> =
        whiteListRepository.addToWhiteList(entry)
}

class RemoveFromWhiteListUseCase @Inject constructor(
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(packageName: String): Result<Unit> =
        whiteListRepository.removeFromWhiteList(packageName)
}

class UpdateWhiteListEntryUseCase @Inject constructor(
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(entry: WhiteListEntry): Result<Unit> =
        whiteListRepository.updateWhiteListEntry(entry)
}

class CheckWhiteListUseCase @Inject constructor(
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(packageName: String): Boolean =
        whiteListRepository.isInWhiteList(packageName)
}

class CreateAuthRequestUseCase @Inject constructor(
    private val authRequestRepository: AuthRequestRepository
) {
    suspend operator fun invoke(
        packageName: String,
        appName: String,
        command: String?,
        path: String?,
        uid: Int,
        pid: Int
    ): AuthRequest = authRequestRepository.createAuthRequest(
        packageName, appName, command, path, uid, pid
    )
}

class GetPendingRequestsUseCase @Inject constructor(
    private val authRequestRepository: AuthRequestRepository
) {
    suspend operator fun invoke(): List<AuthRequest> =
        authRequestRepository.getPendingRequests()
    fun observe(): Flow<List<AuthRequest>> = authRequestRepository.observeAuthRequests()
}

class ApproveAuthRequestUseCase @Inject constructor(
    private val authRequestRepository: AuthRequestRepository,
    private val whiteListRepository: WhiteListRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> {
        val request = authRequestRepository.getRequest(requestId)
            ?: return Result.failure(Exception("Request not found"))

        val entry = WhiteListEntry(
            packageName = request.fromPackage,
            appName = request.fromAppName,
            uid = request.uid,
            grantedTime = System.currentTimeMillis(),
            lastUsed = System.currentTimeMillis(),
            allowAllCommands = request.requestedCommand == null,
            allowedCommands = if (request.requestedCommand != null) listOf(request.requestedCommand) else emptyList()
        )

        whiteListRepository.addToWhiteList(entry)
        return authRequestRepository.approveRequest(requestId)
    }
}

class DenyAuthRequestUseCase @Inject constructor(
    private val authRequestRepository: AuthRequestRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> =
        authRequestRepository.denyRequest(requestId)
}

class ClearAuthHistoryUseCase @Inject constructor(
    private val authRequestRepository: AuthRequestRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRequestRepository.clearHistory()
}

class InstallFakeSuUseCase @Inject constructor(
    private val suBinaryRepository: SuBinaryRepository
) {
    suspend operator fun invoke(targetPath: String): Result<SuBinary> =
        suBinaryRepository.installFakeSu(targetPath)
}

class SwitchToRealSuUseCase @Inject constructor(
    private val suBinaryRepository: SuBinaryRepository,
    private val whiteListRepository: WhiteListRepository,
    private val authRequestRepository: AuthRequestRepository
) {
    suspend operator fun invoke(
        fakePath: String,
        packageName: String,
        command: String?,
        uid: Int,
        pid: Int
    ): Result<Unit> {
        val isAllowed = whiteListRepository.isInWhiteList(packageName)
        if (!isAllowed) {
            authRequestRepository.createAuthRequest(
                packageName = packageName,
                appName = packageName,
                command = command,
                path = fakePath,
                uid = uid,
                pid = pid
            )
            return Result.failure(Exception("Not authorized - request pending"))
        }
        return suBinaryRepository.switchToRealSu(fakePath)
    }
}

class PatchBootImageUseCase @Inject constructor(
    private val bootManagerRepository: BootManagerRepository
) {
    suspend operator fun invoke(): Result<ByteArray> {
        bootManagerRepository.saveBootStage(
            BootStage(Stage.PATCHING, false, "Starting boot image patch", System.currentTimeMillis())
        )
        return bootManagerRepository.patchBootImage()
    }
}

class BackupBootImageUseCase @Inject constructor(
    private val bootManagerRepository: BootManagerRepository
) {
    suspend operator fun invoke(): Result<ByteArray> = bootManagerRepository.backupBootImage()
}

class RestoreBootImageUseCase @Inject constructor(
    private val bootManagerRepository: BootManagerRepository
) {
    suspend operator fun invoke(backup: ByteArray): Result<Unit> =
        bootManagerRepository.restoreBootImage(backup)
}

class VerifySuIntegrityUseCase @Inject constructor(
    private val suBinaryRepository: SuBinaryRepository
) {
    suspend operator fun invoke(path: String): Boolean =
        suBinaryRepository.verifySuIntegrity(path)
}
