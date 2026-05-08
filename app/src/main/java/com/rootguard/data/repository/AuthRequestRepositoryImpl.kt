package com.rootguard.data.repository

import android.content.Context
import com.rootguard.domain.model.AuthRequest
import com.rootguard.domain.model.RequestStatus
import com.rootguard.domain.repository.AuthRequestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRequestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRequestRepository {

    private val prefs = context.getSharedPreferences("auth_requests", Context.MODE_PRIVATE)
    private val _requestsFlow = MutableStateFlow<List<AuthRequest>>(emptyList())

    init {
        refreshFlow()
    }

    private fun refreshFlow() {
        _requestsFlow.value = getRequestHistoryInternal()
    }

    private fun getRequestHistoryInternal(): List<AuthRequest> {
        val json = prefs.getString("history", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val requests = mutableListOf<AuthRequest>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                requests.add(
                    AuthRequest(
                        requestId = obj.getString("request_id"),
                        fromPackage = obj.getString("from_package"),
                        fromAppName = obj.getString("from_app_name"),
                        requestedCommand = obj.optString("command", null),
                        requestedPath = obj.optString("path", null),
                        uid = obj.getInt("uid"),
                        pid = obj.getInt("pid"),
                        timestamp = obj.getLong("timestamp"),
                        status = RequestStatus.valueOf(obj.getString("status"))
                    )
                )
            }
            requests.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createAuthRequest(
        packageName: String,
        appName: String,
        command: String?,
        path: String?,
        uid: Int,
        pid: Int
    ): AuthRequest = withContext(Dispatchers.IO) {
        val request = AuthRequest(
            requestId = generateRequestId(),
            fromPackage = packageName,
            fromAppName = appName,
            requestedCommand = command,
            requestedPath = path,
            uid = uid,
            pid = pid,
            timestamp = System.currentTimeMillis(),
            status = RequestStatus.PENDING
        )

        saveRequest(request)
        refreshFlow()
        request
    }

    private fun saveRequest(request: AuthRequest) {
        val history = getRequestHistoryInternal().toMutableList()
        val existingIndex = history.indexOfFirst { it.requestId == request.requestId }
        if (existingIndex >= 0) {
            history[existingIndex] = request
        } else {
            history.add(0, request)
        }

        val jsonArray = JSONArray()
        history.take(100).forEach { req ->
            jsonArray.put(
                JSONObject().apply {
                    put("request_id", req.requestId)
                    put("from_package", req.fromPackage)
                    put("from_app_name", req.fromAppName)
                    put("command", req.requestedCommand ?: "")
                    put("path", req.requestedPath ?: "")
                    put("uid", req.uid)
                    put("pid", req.pid)
                    put("timestamp", req.timestamp)
                    put("status", req.status.name)
                }
            )
        }

        prefs.edit().putString("history", jsonArray.toString()).apply()
    }

    override suspend fun getPendingRequests(): List<AuthRequest> = withContext(Dispatchers.IO) {
        getRequestHistoryInternal().filter { it.status == RequestStatus.PENDING }
    }

    override suspend fun getRequestHistory(): List<AuthRequest> = withContext(Dispatchers.IO) {
        getRequestHistoryInternal()
    }

    override suspend fun approveRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = getRequest(requestId)
                ?: return@withContext Result.failure(Exception("Request not found"))

            val updatedRequest = request.copy(status = RequestStatus.ALLOWED)
            saveRequest(updatedRequest)

            notifySuDaemon(requestId, true)
            refreshFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = getRequest(requestId)
                ?: return@withContext Result.failure(Exception("Request not found"))

            val updatedRequest = request.copy(status = RequestStatus.DENIED)
            saveRequest(updatedRequest)

            notifySuDaemon(requestId, false)
            refreshFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRequest(requestId: String): AuthRequest? = withContext(Dispatchers.IO) {
        getRequestHistoryInternal().find { it.requestId == requestId }
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString("history", "[]").apply()
            refreshFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAuthRequests(): Flow<List<AuthRequest>> = _requestsFlow

    private fun notifySuDaemon(requestId: String, approved: Boolean) {
        try {
            val socketFile = File(context.filesDir, "su_socket")
            if (socketFile.exists()) {
                socketFile.writeText("$requestId:$approved")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
}
