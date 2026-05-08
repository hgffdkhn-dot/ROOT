package com.rootguard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rootguard.domain.model.WhiteListEntry
import com.rootguard.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhiteListViewModel @Inject constructor(
    private val getWhiteListUseCase: GetWhiteListUseCase,
    private val addToWhiteListUseCase: AddToWhiteListUseCase,
    private val removeFromWhiteListUseCase: RemoveFromWhiteListUseCase,
    private val updateWhiteListEntryUseCase: UpdateWhiteListEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteListUiState())
    val uiState: StateFlow<WhiteListUiState> = _uiState.asStateFlow()

    init {
        loadWhiteList()
        observeWhiteList()
    }

    private fun loadWhiteList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entries = getWhiteListUseCase()
                _uiState.update { it.copy(entries = entries, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun observeWhiteList() {
        viewModelScope.launch {
            getWhiteListUseCase.observe().collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
    }

    fun addToWhiteList(packageName: String, appName: String, allowAll: Boolean) {
        viewModelScope.launch {
            try {
                val entry = WhiteListEntry(
                    packageName = packageName,
                    appName = appName,
                    uid = 0,
                    grantedTime = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis(),
                    allowAllCommands = allowAll,
                    allowedCommands = emptyList()
                )
                addToWhiteListUseCase(entry)
                loadWhiteList()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeFromWhiteList(packageName: String) {
        viewModelScope.launch {
            try {
                removeFromWhiteListUseCase(packageName)
                loadWhiteList()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateWhiteListEntry(entry: WhiteListEntry) {
        viewModelScope.launch {
            try {
                updateWhiteListEntryUseCase(entry)
                loadWhiteList()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class WhiteListUiState(
    val entries: List<WhiteListEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
