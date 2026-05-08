package com.rootguard.data.repository

import android.content.Context
import com.rootguard.domain.model.WhiteListEntry
import com.rootguard.domain.repository.WhiteListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhiteListRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WhiteListRepository {

    private val prefs = context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)
    private val _whiteListFlow = MutableStateFlow<List<WhiteListEntry>>(emptyList())

    init {
        refreshFlow()
    }

    private fun refreshFlow() {
        _whiteListFlow.value = getWhiteListInternal()
    }

    private fun getWhiteListInternal(): List<WhiteListEntry> {
        val entries = mutableListOf<WhiteListEntry>()
        prefs.all.forEach { (packageName, value) ->
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val map = value as Map<String, Any?>
                entries.add(
                    WhiteListEntry(
                        packageName = packageName,
                        appName = map["app_name"] as? String ?: packageName,
                        uid = (map["uid"] as? Number)?.toInt() ?: 0,
                        grantedTime = (map["granted_time"] as? Number)?.toLong() ?: 0L,
                        lastUsed = (map["last_used"] as? Number)?.toLong() ?: 0L,
                        allowAllCommands = map["allow_all"] as? Boolean ?: false,
                        allowedCommands = (map["commands"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                )
            }
        }
        return entries.sortedByDescending { it.grantedTime }
    }

    override suspend fun getWhiteList(): List<WhiteListEntry> = withContext(Dispatchers.IO) {
        getWhiteListInternal()
    }

    override suspend fun addToWhiteList(entry: WhiteListEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entryMap = mapOf(
                "app_name" to entry.appName,
                "uid" to entry.uid,
                "granted_time" to entry.grantedTime,
                "last_used" to entry.lastUsed,
                "allow_all" to entry.allowAllCommands,
                "commands" to entry.allowedCommands
            )

            prefs.edit().apply {
                putString(entry.packageName, serializeMap(entryMap))
                apply()
            }
            refreshFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromWhiteList(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().apply {
                remove(packageName)
                apply()
            }
            refreshFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWhiteListEntry(entry: WhiteListEntry): Result<Unit> = withContext(Dispatchers.IO) {
        addToWhiteList(entry)
    }

    override suspend fun isInWhiteList(packageName: String): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(packageName)
    }

    override suspend fun getWhiteListEntry(packageName: String): WhiteListEntry? = withContext(Dispatchers.IO) {
        getWhiteListInternal().find { it.packageName == packageName }
    }

    override fun observeWhiteList(): Flow<List<WhiteListEntry>> = _whiteListFlow

    private fun serializeMap(map: Map<String, Any?>): String {
        return map.entries.joinToString(";") { (k, v) ->
            when (v) {
                is List<*> -> "$k=${v.joinToString(",")}"
                else -> "$k=$v"
            }
        }
    }
}
