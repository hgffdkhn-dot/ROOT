package com.rootguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.rootguard.domain.model.Settings
import com.rootguard.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("root_settings", Context.MODE_PRIVATE)
    private val _settingsFlow = MutableStateFlow(loadSettings())

    override suspend fun getSettings(): Settings = _settingsFlow.value

    override suspend fun updateSettings(settings: Settings) {
        _settingsFlow.value = settings
        saveSettings(settings)
    }

    override fun observeSettings(): Flow<Settings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): Settings {
        return Settings(
            autoMonitor = prefs.getBoolean("auto_monitor", true),
            notifyOnRequest = prefs.getBoolean("notify_on_request", true),
            darkMode = prefs.getBoolean("dark_mode", false)
        )
    }

    private fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean("auto_monitor", settings.autoMonitor)
            .putBoolean("notify_on_request", settings.notifyOnRequest)
            .putBoolean("dark_mode", settings.darkMode)
            .apply()
    }
}
