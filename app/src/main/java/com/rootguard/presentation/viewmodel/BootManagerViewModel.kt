package com.rootguard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rootguard.domain.model.BootStage
import com.rootguard.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BootManagerViewModel @Inject constructor(
    private val patchBootImageUseCase: PatchBootImageUseCase,
    private val backupBootImageUseCase: BackupBootImageUseCase,
    private val restoreBootImageUseCase: RestoreBootImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BootManagerUiState())
    val uiState: StateFlow<BootManagerUiState> = _uiState.asStateFlow()

    private val _bootStage = MutableStateFlow<BootStage?>(null)
    val bootStage: StateFlow<BootStage?> = _bootStage.asStateFlow()

    init {
        checkBackupStatus()
    }

    private fun checkBackupStatus() {
        viewModelScope.launch {
            val hasBackup = checkBackupExists()
            _uiState.update { it.copy(hasBackup = hasBackup) }
        }
    }

    private suspend fun checkBackupExists(): Boolean {
        return try {
            val backupDir = java.io.File("/data/data/com.rootguard/files/backups")
            backupDir.exists() && backupDir.listFiles()?.isNotEmpty() ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun patchBootImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            try {
                val result = patchBootImageUseCase()
                if (result.isSuccess) {
                    _uiState.update { it.copy(isProcessing = false) }
                } else {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        error = result.exceptionOrNull()?.message ?: "修改失败"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isProcessing = false,
                    error = e.message ?: "修改失败"
                ) }
            }
        }
    }

    fun backupBootImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            try {
                val result = backupBootImageUseCase()
                if (result.isSuccess) {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        hasBackup = true
                    ) }
                } else {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        error = result.exceptionOrNull()?.message ?: "备份失败"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isProcessing = false,
                    error = e.message ?: "备份失败"
                ) }
            }
        }
    }

    fun restoreBootImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            try {
                val backupResult = backupBootImageUseCase()
                if (backupResult.isSuccess) {
                    val restoreResult = restoreBootImageUseCase(backupResult.getOrThrow())
                    if (restoreResult.isSuccess) {
                        _uiState.update { it.copy(
                            isProcessing = false,
                            hasBackup = false
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isProcessing = false,
                            error = restoreResult.exceptionOrNull()?.message ?: "恢复失败"
                        ) }
                    }
                } else {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        error = "无法读取备份"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isProcessing = false,
                    error = e.message ?: "恢复失败"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class BootManagerUiState(
    val isProcessing: Boolean = false,
    val hasBackup: Boolean = false,
    val error: String? = null
)
