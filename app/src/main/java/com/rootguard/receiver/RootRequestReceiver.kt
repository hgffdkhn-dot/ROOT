package com.rootguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rootguard.service.RootPermissionService

class RootRequestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.rootguard.ROOT_REQUEST") {
            val packageName = intent.getStringExtra("package_name") ?: return
            val appName = intent.getStringExtra("app_name") ?: packageName
            val command = intent.getStringExtra("command")

            val serviceIntent = Intent(context, RootPermissionService::class.java).apply {
                action = RootPermissionService.ACTION_ROOT_REQUEST
                putExtra(RootPermissionService.EXTRA_PACKAGE_NAME, packageName)
                putExtra(RootPermissionService.EXTRA_APP_NAME, appName)
                putExtra(RootPermissionService.EXTRA_COMMAND, command)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
