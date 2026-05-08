package com.rootguard.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rootguard.presentation.components.AppListItem
import com.rootguard.presentation.components.ConfirmDialog
import com.rootguard.presentation.viewmodel.AppsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGrantDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showRevokeDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "应用管理",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.searchApps(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索应用...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchApps("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除"
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已安装应用",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = "${uiState.filteredApps.size} 个应用",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredApps) { app ->
                    AppListItem(
                        app = app,
                        onGrantClick = { showGrantDialog = app.packageName to app.appName },
                        onRevokeClick = { showRevokeDialog = app.packageName to app.appName }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    showGrantDialog?.let { (packageName, appName) ->
        ConfirmDialog(
            title = "确认授予Root权限",
            message = "确定要授予 $appName Root权限吗？\n\n警告: 授予Root权限可能会带来安全风险",
            onConfirm = {
                viewModel.grantRoot(packageName, appName)
                showGrantDialog = null
            },
            onDismiss = {
                showGrantDialog = null
            }
        )
    }

    showRevokeDialog?.let { (packageName, appName) ->
        ConfirmDialog(
            title = "确认撤销Root权限",
            message = "确定要撤销 $appName 的Root权限吗？",
            onConfirm = {
                viewModel.revokeRoot(packageName, appName)
                showRevokeDialog = null
            },
            onDismiss = {
                showRevokeDialog = null
            }
        )
    }
}
