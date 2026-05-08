package com.rootguard.presentation.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.rootguard.domain.model.AppInfo
import com.rootguard.domain.model.LogAction
import com.rootguard.domain.model.RootStatus
import com.rootguard.domain.model.RootType
import com.rootguard.domain.model.SecurityLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RootStatusCard(
    rootStatus: RootStatus,
    onCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rootStatus.isRooted) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (rootStatus.isRooted) Icons.Default.Security else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (rootStatus.isRooted) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (rootStatus.isRooted) "设备已Root" else "设备未Root",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (rootStatus.isRooted) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (rootStatus.isRooted) {
                Text(
                    text = when (rootStatus.rootType) {
                        RootType.MAGISK -> "Magisk Root"
                        RootType.SUPERUSER -> "Superuser Root"
                        RootType.FULL_ROOT -> "Full Root"
                        RootType.PARTIAL_ROOT -> "Partial Root"
                        RootType.NONE -> "Unknown"
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                rootStatus.suVersion?.let {
                    Text(
                        text = "SU版本: $it",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rootStatus.isRooted) Color(0xFF4CAF50) else Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新检测")
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    onGrantClick: () -> Unit,
    onRevokeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                icon = app.icon,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = app.packageName,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (app.hasRootAccess) {
                IconButton(onClick = onRevokeClick) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = "撤销Root",
                        tint = Color(0xFFF44336)
                    )
                }
            } else {
                IconButton(onClick = onGrantClick) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "授予Root",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun AppIcon(
    icon: Drawable?,
    modifier: Modifier = Modifier
) {
    if (icon != null) {
        Image(
            bitmap = icon.toBitmap(96, 96).asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun LogListItem(
    log: SecurityLog,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (log.action) {
                    LogAction.GRANT_ROOT -> Icons.Default.CheckCircle
                    LogAction.REVOKE_ROOT -> Icons.Default.Cancel
                    LogAction.REQUEST_ROOT -> Icons.Default.Help
                    LogAction.ROOT_DETECTED -> Icons.Default.Security
                    LogAction.ROOT_REMOVED -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when (log.action) {
                    LogAction.GRANT_ROOT -> Color(0xFF4CAF50)
                    LogAction.REVOKE_ROOT -> Color(0xFFF44336)
                    LogAction.REQUEST_ROOT -> Color(0xFF2196F3)
                    LogAction.ROOT_DETECTED -> Color(0xFF4CAF50)
                    LogAction.ROOT_REMOVED -> Color(0xFFF44336)
                },
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = when (log.action) {
                        LogAction.GRANT_ROOT -> "Root权限授予"
                        LogAction.REVOKE_ROOT -> "Root权限撤销"
                        LogAction.REQUEST_ROOT -> "Root权限请求"
                        LogAction.ROOT_DETECTED -> "检测到Root"
                        LogAction.ROOT_REMOVED -> "Root已移除"
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = formatTimestamp(log.timestamp),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认", color = Color(0xFFF44336))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
