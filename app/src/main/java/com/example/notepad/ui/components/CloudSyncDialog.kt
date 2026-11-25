package com.example.notepad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.sync.CloudSyncUiState
import com.example.notepad.ui.theme.LocalCustomButtonColors

/**
 * Google 雲端硬碟同步設定與狀態管理對話框
 *
 * 顯示網路連線狀態、Google 帳號綁定狀態、待同步異動筆數與最後同步時間，
 * 提供連結／切換／中斷 Google 帳號、自動同步開關、儲存設定與「立即雙向同步」操作。
 */
@Composable
fun CloudSyncDialog(
    uiState: CloudSyncUiState,
    onAuthorizeGoogleDrive: () -> Unit,
    onDisconnectGoogleDrive: () -> Unit,
    isAuthorizingGoogle: Boolean = false,
    googleAuthError: String? = null,
    onSaveConfig: (syncEnabled: Boolean) -> Unit,
    onSyncNow: (syncEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSettings = uiState.syncSettings
    var syncEnabled by remember { mutableStateOf(initialSettings.syncEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 標題列
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            uiState.isSyncing -> Icons.Default.CloudSync
                            !uiState.isOnline ||
                                !uiState.syncSettings.syncEnabled ||
                                !uiState.isCloudConfigured -> Icons.Default.CloudOff
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        tint = if (uiState.isOnline && uiState.isCloudConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "Google 雲端硬碟同步",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 同步狀態摘要卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val networkLabel = if (uiState.isOnline) {
                            "🌐 網路狀態：已連線"
                        } else {
                            "📡 網路狀態：離線中（已保留本機異動）"
                        }
                        Text(
                            text = networkLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )

                        val engineReady = initialSettings.googleAccountEmail.isNotBlank() || uiState.isCloudConfigured
                        val engineLabel = if (initialSettings.googleAccountEmail.isNotBlank()) {
                            "☁️ 雲端服務：Google 雲端硬碟（${initialSettings.googleAccountEmail}）"
                        } else {
                            "⚠️ 雲端服務：尚未連結 Google 帳號"
                        }
                        Text(
                            text = engineLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (engineReady) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )

                        val pendingLabel = if (uiState.pendingCount > 0) {
                            "⏳ 待同步異動：${uiState.pendingCount} 筆（含記事/任務/刪除/設定）"
                        } else {
                            "✅ 待同步異動：無待上傳資料"
                        }
                        Text(
                            text = pendingLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (uiState.pendingCount > 0) FontWeight.Bold else FontWeight.Normal
                        )

                        val lastSyncText = uiState.syncSettings.lastSyncAt?.let {
                            DateFormatter.formatDateTime(it)
                        } ?: "尚未成功同步"
                        Text(
                            text = "🕒 最後同步時間：$lastSyncText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "📝 同步狀態：${uiState.lastRunSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 雲端同步總開關
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "啟用自動與手動雲端同步",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "本地優先儲存，網路恢復後自動上傳，衝突採最新修改時間優先智慧合併。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { syncEnabled = it }
                        )
                    }
                }

                // Google 帳號綁定操作區
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val linkedEmail = initialSettings.googleAccountEmail
                                val linkedName = initialSettings.googleDisplayName
                                Text(
                                    text = when {
                                        linkedName.isNotBlank() && linkedEmail.isNotBlank() ->
                                            "$linkedName（$linkedEmail）"
                                        linkedEmail.isNotBlank() -> linkedEmail
                                        else -> "尚未連結 Google 帳號"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "資料將安全儲存在 Google 雲端硬碟專用隱藏空間（appDataFolder），登入同一帳號即可跨手機同步。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAuthorizeGoogleDrive,
                                enabled = !isAuthorizingGoogle && !uiState.isSyncing,
                                colors = AppButtonThemeDefaults.filledColors(),
                                border = AppButtonThemeDefaults.filledBorder(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isAuthorizingGoogle) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalCustomButtonColors.current.contentColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("授權連結中...")
                                } else {
                                    Text(
                                        if (initialSettings.googleAccountEmail.isNotBlank()) {
                                            "重新授權 / 切換帳號"
                                        } else {
                                            "連結 Google 帳號"
                                        }
                                    )
                                }
                            }

                            if (initialSettings.hasGoogleDriveConfig()) {
                                OutlinedButton(
                                    onClick = onDisconnectGoogleDrive,
                                    enabled = !isAuthorizingGoogle && !uiState.isSyncing,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("中斷")
                                }
                            }
                        }
                    }
                }

                googleAuthError?.let { authErr ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authErr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // 操作按鈕列
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSyncNow(syncEnabled) },
                        enabled = !uiState.isSyncing && !isAuthorizingGoogle,
                        colors = AppButtonThemeDefaults.filledColors(),
                        border = AppButtonThemeDefaults.filledBorder(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalCustomButtonColors.current.contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在雙向同步...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("立即雙向同步全部資料")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("關閉")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onSaveConfig(syncEnabled) }
                        ) {
                            Text("儲存設定")
                        }
                    }
                }
            }
        }
    }
}
