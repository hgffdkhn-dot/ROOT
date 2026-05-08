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
import com.rootguard.presentation.components.RootStatusCard
import com.rootguard.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRevokeDialog by remember { mutableStateOf<String?>(null) }
    var revokeAppName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Root权限守护者",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    RootStatusCard(
                        rootStatus = uiState.rootStatus,
                        onCheckClick = { viewModel.checkRootStatus() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已获取Root的应用",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "${uiState.appsWithRoot.size} 个",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (uiState.appsWithRoot.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "暂无应用获取Root权限",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.appsWithRoot) { app ->
                        AppListItem(
                            app = app,
                            onGrantClick = { viewModel.grantRoot(app.packageName, app.appName) },
                            onRevokeClick = {
                                revokeAppName = app.appName
                                showRevokeDialog = app.packageName
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    showRevokeDialog?.let { packageName ->
        ConfirmDialog(
            title = "确认撤销Root权限",
            message = "确定要撤销 $revokeAppName 的Root权限吗？",
            onConfirm = {
                viewModel.revokeRoot(packageName, revokeAppName)
                showRevokeDialog = null
            },
            onDismiss = {
                showRevokeDialog = null
            }
        )
    }
}
