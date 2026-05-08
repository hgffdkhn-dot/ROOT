package com.rootguard.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rootguard.domain.model.WhiteListEntry
import com.rootguard.presentation.viewmodel.WhiteListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteListScreen(
    viewModel: WhiteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<WhiteListEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("白名单管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无白名单应用", color = Color.Gray)
                        Text(
                            "添加应用到白名单后，这些应用可以自动获取 Root 权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.entries, key = { it.packageName }) { entry ->
                        WhiteListItem(
                            entry = entry,
                            onEdit = {
                                selectedEntry = entry
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.removeFromWhiteList(entry.packageName)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWhiteListDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { packageName, appName, allowAll ->
                viewModel.addToWhiteList(packageName, appName, allowAll)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedEntry != null) {
        EditWhiteListDialog(
            entry = selectedEntry!!,
            onDismiss = {
                showEditDialog = false
                selectedEntry = null
            },
            onConfirm = { updatedEntry ->
                viewModel.updateWhiteListEntry(updatedEntry)
                showEditDialog = false
                selectedEntry = null
            }
        )
    }
}

@Composable
fun WhiteListItem(
    entry: WhiteListEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.appName,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entry.allowAllCommands) {
                        AssistChip(
                            onClick = { },
                            label = { Text("完全授权", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                labelColor = Color(0xFF4CAF50)
                            )
                        )
                    } else {
                        AssistChip(
                            onClick = { },
                            label = { Text("命令限制", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                                labelColor = Color(0xFFFF9800)
                            )
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "编辑", tint = Color.Gray)
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "删除", tint = Color.Red)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要从白名单中移除 ${entry.appName} 吗？移除后该应用将需要重新申请授权。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AddWhiteListDialog(
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, appName: String, allowAll: Boolean) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var allowAll by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加白名单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("包名") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("应用名称") },
                    placeholder = { Text("示例应用") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowAll,
                        onCheckedChange = { allowAll = it }
                    )
                    Text("允许所有命令")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(packageName, appName, allowAll) },
                enabled = packageName.isNotBlank() && appName.isNotBlank()
            ) {
                Text("添加")
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
fun EditWhiteListDialog(
    entry: WhiteListEntry,
    onDismiss: () -> Unit,
    onConfirm: (WhiteListEntry) -> Unit
) {
    var allowAll by remember { mutableStateOf(entry.allowAllCommands) }
    var commands by remember { mutableStateOf(entry.allowedCommands.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑白名单 - ${entry.appName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowAll,
                        onCheckedChange = { allowAll = it }
                    )
                    Text("允许所有命令")
                }

                if (!allowAll) {
                    OutlinedTextField(
                        value = commands,
                        onValueChange = { commands = it },
                        label = { Text("允许的命令（每行一个）") },
                        placeholder = { Text("ls\ncat /data/data\n") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        entry.copy(
                            allowAllCommands = allowAll,
                            allowedCommands = if (allowAll) emptyList()
                                              else commands.split("\n").filter { it.isNotBlank() }
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
