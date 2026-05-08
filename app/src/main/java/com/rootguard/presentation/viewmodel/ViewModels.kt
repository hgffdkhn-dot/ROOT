package com.rootguard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rootguard.domain.model.AppInfo
import com.rootguard.domain.model.RootStatus
import com.rootguard.domain.model.RootType
import com.rootguard.domain.model.SecurityLog
import com.rootguard.domain.model.Settings
import com.rootguard.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val checkRootStatusUseCase: CheckRootStatusUseCase,
    private val getAppsWithRootUseCase: GetAppsWithRootUseCase,
    private val grantRootUseCase: GrantRootUseCase,
    private val revokeRootUseCase: RevokeRootUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val rootStatus = checkRootStatusUseCase()
            val appsWithRoot = getAppsWithRootUseCase()

            _uiState.update {
                it.copy(
                    rootStatus = rootStatus,
                    appsWithRoot = appsWithRoot,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            getAppsWithRootUseCase.observe().collect { apps ->
                _uiState.update { it.copy(appsWithRoot = apps) }
            }
        }
    }

    fun checkRootStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rootStatus = checkRootStatusUseCase()
            _uiState.update { it.copy(rootStatus = rootStatus, isLoading = false) }
        }
    }

    fun grantRoot(packageName: String, appName: String) {
        viewModelScope.launch {
            grantRootUseCase(packageName, appName)
            loadData()
        }
    }

    fun revokeRoot(packageName: String, appName: String) {
        viewModelScope.launch {
            revokeRootUseCase(packageName, appName)
            loadData()
        }
    }
}

data class HomeUiState(
    val rootStatus: RootStatus = RootStatus(
        isRooted = false,
        rootType = RootType.NONE,
        lastCheckTime = 0L
    ),
    val appsWithRoot: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val grantRootUseCase: GrantRootUseCase,
    private val revokeRootUseCase: RevokeRootUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apps = getInstalledAppsUseCase()
            _uiState.update { it.copy(apps = apps, isLoading = false) }
        }
    }

    fun grantRoot(packageName: String, appName: String) {
        viewModelScope.launch {
            grantRootUseCase(packageName, appName)
            loadApps()
        }
    }

    fun revokeRoot(packageName: String, appName: String) {
        viewModelScope.launch {
            revokeRootUseCase(packageName, appName)
            loadApps()
        }
    }

    fun searchApps(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredApps: List<AppInfo>
        get() = if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
}

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val getSecurityLogsUseCase: GetSecurityLogsUseCase,
    private val clearLogsUseCase: ClearLogsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logs = getSecurityLogsUseCase()
            _uiState.update { it.copy(logs = logs, isLoading = false) }
        }

        viewModelScope.launch {
            getSecurityLogsUseCase.observe().collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            clearLogsUseCase()
            _uiState.update { it.copy(logs = emptyList()) }
        }
    }
}

data class LogsUiState(
    val logs: List<SecurityLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val settings = getSettingsUseCase()
            _uiState.update { it.copy(settings = settings, isLoading = false) }
        }

        viewModelScope.launch {
            getSettingsUseCase.observe().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun updateAutoMonitor(enabled: Boolean) {
        viewModelScope.launch {
            val newSettings = _uiState.value.settings.copy(autoMonitor = enabled)
            updateSettingsUseCase(newSettings)
        }
    }

    fun updateNotifyOnRequest(enabled: Boolean) {
        viewModelScope.launch {
            val newSettings = _uiState.value.settings.copy(notifyOnRequest = enabled)
            updateSettingsUseCase(newSettings)
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val newSettings = _uiState.value.settings.copy(darkMode = enabled)
            updateSettingsUseCase(newSettings)
        }
    }
}

data class SettingsUiState(
    val settings: Settings = Settings(),
    val isLoading: Boolean = false,
    val error: String? = null
)
