package com.rootguard.domain.model

data class RootConfig(
    val isInstalled: Boolean = false,
    val suPath: String = "",
    val suVersion: String = "",
    val suBinaryPath: String = "",
    val daemonPath: String = "",
    val installationDate: Long = 0L,
    val lastUpdate: Long = 0L
)

data class SuBinary(
    val name: String,
    val path: String,
    val arch: String,
    val size: Long,
    val md5: String,
    val isFake: Boolean = false,
    val permissions: Int = 0
)

data class WhiteListEntry(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val grantedTime: Long,
    val lastUsed: Long,
    val allowAllCommands: Boolean = false,
    val allowedCommands: List<String> = emptyList()
)

data class AuthRequest(
    val requestId: String,
    val fromPackage: String,
    val fromAppName: String,
    val requestedCommand: String?,
    val requestedPath: String?,
    val uid: Int,
    val pid: Int,
    val timestamp: Long,
    val status: RequestStatus = RequestStatus.PENDING
)

enum class RequestStatus {
    PENDING,
    ALLOWED,
    DENIED,
    TIMEOUT
}

data class RootStatus(
    val isRooted: Boolean,
    val hasMagisk: Boolean,
    val hasCustomSu: Boolean,
    val suVersion: String?,
    val rootMethod: RootMethod,
    val lastCheckTime: Long
)

enum class RootMethod {
    NONE,
    MAGISK,
    PHHMSU,
    CUSTOM_SU,
    KERNELSU
}

data class BootStage(
    val stage: Stage,
    val isCompleted: Boolean,
    val message: String,
    val timestamp: Long
)

enum class Stage {
    NONE,
    DETECTING,
    PATCHING,
    INJECTING_SU,
    VERIFYING,
    COMPLETED,
    FAILED
}
