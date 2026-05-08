package com.rootguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rootguard.R
import com.rootguard.domain.model.AuthRequest
import com.rootguard.domain.model.WhiteListEntry
import com.rootguard.domain.repository.AuthRequestRepository
import com.rootguard.domain.repository.WhiteListRepository
import com.rootguard.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class RootPermissionService : Service() {

    @Inject
    lateinit var authRequestRepository: AuthRequestRepository

    @Inject
    lateinit var whiteListRepository: WhiteListRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "root_permission_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_ROOT_REQUEST = "com.rootguard.ROOT_REQUEST"
        const val ACTION_APPROVE = "com.rootguard.APPROVE"
        const val ACTION_DENY = "com.rootguard.DENY"
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_COMMAND = "command"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ROOT_REQUEST -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_NOT_STICKY
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
                val command = intent.getStringExtra(EXTRA_COMMAND)

                handleRootRequest(packageName, appName, command)
            }
            ACTION_APPROVE -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (requestId != null) {
                    approveRequest(requestId)
                }
            }
            ACTION_DENY -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (requestId != null) {
                    denyRequest(requestId)
                }
            }
            else -> {
                startForegroundService()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        if (isRunning) return
        isRunning = true

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Root 权限管理")
            .setContentText("正在监听 Root 权限请求")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Root 权限请求",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Root 权限请求通知"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun handleRootRequest(packageName: String, appName: String, command: String?) {
        serviceScope.launch {
            try {
                val isInWhiteList = whiteListRepository.isInWhiteList(packageName)

                if (isInWhiteList) {
                    autoApproveRequest(packageName, appName, command)
                } else {
                    createAuthRequest(packageName, appName, command)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun autoApproveRequest(packageName: String, appName: String, command: String?) {
        val entry = whiteListRepository.getWhiteListEntry(packageName)
        if (entry != null) {
            val canExecute = entry.allowAllCommands ||
                            (command != null && entry.allowedCommands.contains(command))

            if (canExecute) {
                notifyUser(packageName, appName, "自动授权成功", true)
            } else {
                createAuthRequest(packageName, appName, command)
            }
        }
    }

    private suspend fun createAuthRequest(packageName: String, appName: String, command: String?) {
        val request = authRequestRepository.createAuthRequest(
            packageName = packageName,
            appName = appName,
            command = command,
            path = null,
            uid = android.os.Process.myUid(),
            pid = android.os.Process.myPid()
        )

        notifyUser(packageName, appName, "等待授权", false, request.requestId)
    }

    private fun approveRequest(requestId: String) {
        serviceScope.launch {
            try {
                authRequestRepository.approveRequest(requestId)
                notifyRequestResult(requestId, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun denyRequest(requestId: String) {
        serviceScope.launch {
            try {
                authRequestRepository.denyRequest(requestId)
                notifyRequestResult(requestId, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyUser(
        packageName: String,
        appName: String,
        message: String,
        isApproved: Boolean,
        requestId: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_REQUEST_ID, requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$appName 请求 Root 权限")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!isApproved && requestId != null) {
            val approveIntent = Intent(this, RootPermissionService::class.java).apply {
                action = ACTION_APPROVE
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
            val approvePendingIntent = PendingIntent.getService(
                this,
                1,
                approveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val denyIntent = Intent(this, RootPermissionService::class.java).apply {
                action = ACTION_DENY
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
            val denyPendingIntent = PendingIntent.getService(
                this,
                2,
                denyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_menu_send,
                "允许",
                approvePendingIntent
            ).addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "拒绝",
                denyPendingIntent
            )
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun notifyRequestResult(requestId: String, isApproved: Boolean) {
        val message = if (isApproved) "已授权 Root 权限" else "已拒绝 Root 权限"

        serviceScope.launch {
            val request = authRequestRepository.getRequest(requestId)
            if (request != null) {
                val intent = Intent(this@RootPermissionService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                val pendingIntent = PendingIntent.getActivity(
                    this@RootPermissionService,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(this@RootPermissionService, CHANNEL_ID)
                    .setContentTitle(request.fromAppName)
                    .setContentText(message)
                    .setSmallIcon(if (isApproved) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isRunning = false
    }
}
