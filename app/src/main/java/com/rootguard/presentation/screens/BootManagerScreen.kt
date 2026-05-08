package com.rootguard.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rootguard.domain.model.BootStage
import com.rootguard.domain.model.Stage
import com.rootguard.presentation.viewmodel.BootManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootManagerScreen(
    viewModel: BootManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bootStage by viewModel.bootStage.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boot 管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BootStatusCard(bootStage)

            if (uiState.hasBackup) {
                BackupStatusCard(
                    onRestore = { showRestoreDialog = true }
                )
            }

            ActionButtons(
                isProcessing = uiState.isProcessing,
                onPatch = { showConfirmDialog = true },
                onBackup = { viewModel.backupBootImage() },
                onRestore = { showRestoreDialog = true }
            )

            if (uiState.error != null) {
                ErrorCard(message = uiState.error!!)
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmPatchDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                viewModel.patchBootImage()
                showConfirmDialog = false
            }
        )
    }

    if (showRestoreDialog) {
        ConfirmRestoreDialog(
            onDismiss = { showRestoreDialog = false },
            onConfirm = {
                viewModel.restoreBootImage()
                showRestoreDialog = false
            }
        )
    }
}

@Composable
fun BootStatusCard(stage: BootStage?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        when (stage?.stage) {
                            Stage.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            Stage.FAILED -> Color(0xFFF44336).copy(alpha = 0.1f)
                            Stage.INJECTING_SU, Stage.PATCHING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            else -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        },
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (stage?.stage) {
                        Stage.COMPLETED -> Icons.Default.CheckCircle
                        Stage.FAILED -> Icons.Default.Error
                        Stage.INJECTING_SU, Stage.PATCHING -> Icons.Default.Refresh
                        else -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    tint = when (stage?.stage) {
                        Stage.COMPLETED -> Color(0xFF4CAF50)
                        Stage.FAILED -> Color(0xFFF44336)
                        Stage.INJECTING_SU, Stage.PATCHING -> Color(0xFFFF9800)
                        else -> Color(0xFF2196F3)
                    },
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (stage?.stage) {
                    Stage.DETECTING -> "正在检测..."
                    Stage.PATCHING -> "正在备份..."
                    Stage.INJECTING_SU -> "正在修改..."
                    Stage.VERIFYING -> "正在验证..."
                    Stage.COMPLETED -> "修改完成！"
                    Stage.FAILED -> "修改失败"
                    else -> "未修改"
                },
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stage?.message ?: "点击下方按钮修改 boot 镜像",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun BackupStatusCard(onRestore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backup, "备份", tint = Color(0xFF2196F3))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("已备份 boot 镜像", fontWeight = FontWeight.Bold)
                Text("可随时恢复到原始状态", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("恢复")
            }
        }
    }
}

@Composable
fun ActionButtons(
    isProcessing: Boolean,
    onPatch: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onPatch,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isProcessing) "处理中..." else "修改 Boot 镜像")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBackup,
                modifier = Modifier.weight(1f),
                enabled = !isProcessing
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, "备份", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("备份")
                }
            }

            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.weight(1f),
                enabled = !isProcessing
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, "恢复", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复")
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, "错误", tint = Color(0xFFF44336))
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = Color(0xFFC62828))
        }
    }
}

@Composable
fun ConfirmPatchDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认修改 Boot 镜像") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("此操作将：")
                Text("• 备份当前 boot 镜像")
                Text("• 向 ramdisk 中注入 SU 二进制文件")
                Text("• 修改 init.rc 添加服务")
                Text("• 验证修改确保安全")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ 警告：修改 boot 镜像有风险，请确保设备已解锁 Bootloader，并且你了解可能的后果。",
                    color = Color(0xFFF44336),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Text("确认修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ConfirmRestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认恢复 Boot 镜像") },
        text = {
            Column {
                Text("此操作将恢复 boot 镜像到修改前的状态。")
                Text(
                    "⚠️ 警告：恢复后需要重新获取 Root 权限。",
                    color = Color(0xFFF44336),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336),
                    contentColor = Color.White
                )
            ) {
                Text("确认恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
