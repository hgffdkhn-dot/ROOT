package com.rootguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.rootguard.domain.model.SecurityLog
import com.rootguard.domain.repository.LogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : LogRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("root_logs", Context.MODE_PRIVATE)
    private val _logsFlow = MutableStateFlow<List<SecurityLog>>(loadLogs())

    override suspend fun getLogs(): List<SecurityLog> = _logsFlow.value

    override suspend fun addLog(log: SecurityLog) {
        val currentLogs = _logsFlow.value.toMutableList()
        currentLogs.add(0, log)
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logsFlow.value = currentLogs
        saveLogs(currentLogs)
    }

    override suspend fun clearLogs() {
        _logsFlow.value = emptyList()
        prefs.edit().clear().apply()
    }

    override fun observeLogs(): Flow<List<SecurityLog>> = _logsFlow.asStateFlow()

    private fun loadLogs(): List<SecurityLog> {
        val logsJson = prefs.getString("logs", "[]") ?: "[]"
        return try {
            parseLogs(logsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLogs(logs: List<SecurityLog>) {
        val logsJson = serializeLogs(logs)
        prefs.edit().putString("logs", logsJson).apply()
    }

    private fun parseLogs(json: String): List<SecurityLog> {
        if (json == "[]" || json.isEmpty()) return emptyList()
        return try {
            val logs = mutableListOf<SecurityLog>()
            val items = json.trim().removePrefix("[").removeSuffix("]").split("},")
            items.forEachIndexed { index, item ->
                val cleanItem = if (!item.endsWith("}")) item + "}" else item
                try {
                    val parts = cleanItem.split(",")
                    val id = parts.getOrNull(0)?.substringAfter("\"id\":")?.toLongOrNull() ?: index.toLong()
                    val packageName = parts.getOrNull(1)?.substringAfter("\"packageName\":\"")?.substringBefore("\"") ?: ""
                    val appName = parts.getOrNull(2)?.substringAfter("\"appName\":\"")?.substringBefore("\"") ?: ""
                    val timestamp = parts.getOrNull(4)?.substringAfter("\"timestamp\":")?.toLongOrNull() ?: 0L
                    logs.add(
                        SecurityLog(
                            id = id,
                            packageName = packageName,
                            appName = appName,
                            action = com.rootguard.domain.model.LogAction.GRANT_ROOT,
                            timestamp = timestamp
                        )
                    )
                } catch (e: Exception) {
                }
            }
            logs
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeLogs(logs: List<SecurityLog>): String {
        if (logs.isEmpty()) return "[]"
        return logs.joinToString(",", "[", "]") { log ->
            """{"id":${log.id},"packageName":"${log.packageName}","appName":"${log.appName}","action":"${log.action}","timestamp":${log.timestamp}}"""
        }
    }

    companion object {
        private const val MAX_LOGS = 100
    }
}
