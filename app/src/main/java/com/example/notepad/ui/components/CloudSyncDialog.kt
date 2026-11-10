package com.example.notepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.sync.CloudSyncUiState
import java.util.UUID

/**
 * Firebase 雲端資料同步設定與狀態管理對話框
 *
 * 顯示目前連線狀態、待同步異動筆數、最後同步時間、同步空間代碼與選填的手動 Firebase 參數，
 * 並提供「儲存設定」與一鍵「立即同步」操作（遵循操作規範 4、5、7）。
 */
@Composable
fun CloudSyncDialog(
    uiState: CloudSyncUiState,
    onSaveConfig: (
        syncEnabled: Boolean,
        syncAccountId: String,
        firebaseProjectId: String,
        firebaseAppId: String,
        firebaseApiKey: String
    ) -> Unit,
    onSyncNow: (
        syncEnabled: Boolean,
        syncAccountId: String,
        firebaseProjectId: String,
        firebaseAppId: String,
        firebaseApiKey: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialSettings = uiState.syncSettings

    var syncEnabled by remember { mutableStateOf(initialSettings.syncEnabled) }
    var syncAccountId by remember { mutableStateOf(initialSettings.syncAccountId) }
    var firebaseProjectId by remember { mutableStateOf(initialSettings.firebaseProjectId) }
    var firebaseAppId by remember { mutableStateOf(initialSettings.firebaseAppId) }
    var firebaseApiKey by remember { mutableStateOf(initialSettings.firebaseApiKey) }
    var showFirebaseConfigForm by remember {
        mutableStateOf(initialSettings.hasManualFirebaseConfig() || !uiState.isFirebaseConfigured)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    // 若初始進入對話框時系統剛生成預設同步代碼，同步補入顯示欄位
    LaunchedEffect(uiState.syncSettings.syncAccountId) {
        if (syncAccountId.isBlank() && uiState.syncSettings.syncAccountId.isNotBlank()) {
            syncAccountId = uiState.syncSettings.syncAccountId
        }
    }

    fun validateInputs(): Boolean {
        val anyManualFilled = firebaseProjectId.isNotBlank() ||
            firebaseAppId.isNotBlank() ||
            firebaseApiKey.isNotBlank()
        val allManualFilled = firebaseProjectId.isNotBlank() &&
            firebaseAppId.isNotBlank() &&
            firebaseApiKey.isNotBlank()
        if (anyManualFilled && !allManualFilled) {
            validationError = "若要手動設定 Firebase，請完整填寫 Project ID、App ID 與 Web API Key（或全部留空改用 google-services.json）。"
            showFirebaseConfigForm = true
            showFeedback(context, validationError!!)
            return false
        }
        validationError = null
        return true
    }

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
                            !uiState.isOnline || !uiState.syncSettings.syncEnabled -> Icons.Default.CloudOff
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        tint = if (uiState.isOnline && uiState.isFirebaseConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "Firebase 雲端資料同步",
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

                        val firebaseLabel = if (uiState.isFirebaseConfigured) {
                            "🔥 雲端引擎：Firebase Firestore 已就緒"
                        } else {
                            "⚠️ 雲端引擎：尚未設定 Firebase 專案"
                        }
                        Text(
                            text = firebaseLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isFirebaseConfigured) {
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

                // 跨裝置同步空間代碼
                OutlinedTextField(
                    value = syncAccountId,
                    onValueChange = { syncAccountId = it },
                    label = { Text("同步空間代碼（跨裝置識別 ID）") },
                    supportingText = {
                        Text("不同裝置填寫相同代碼即可雙向同步全部資料；留空儲存將自動產生。")
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                syncAccountId = "notepad-" + UUID.randomUUID().toString().take(8)
                                showFeedback(context, "已產生新的同步空間代碼")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新產生同步代碼"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 折疊式手動 Firebase 專案設定表單（支援無 google-services.json 環境）
                OutlinedButton(
                    onClick = { showFirebaseConfigForm = !showFirebaseConfigForm },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (showFirebaseConfigForm) {
                            "隱藏手動 Firebase 連線參數"
                        } else {
                            "設定手動 Firebase 連線參數（選填）"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showFirebaseConfigForm) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = showFirebaseConfigForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "若已於 app/ 加入 google-services.json 可全部留空；否則請貼上 Firebase Console 的專案連線資訊：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = firebaseProjectId,
                            onValueChange = {
                                firebaseProjectId = it
                                validationError = null
                            },
                            label = { Text("Firebase Project ID") },
                            placeholder = { Text("例如：my-notepad-sync") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = firebaseAppId,
                            onValueChange = {
                                firebaseAppId = it
                                validationError = null
                            },
                            label = { Text("Firebase App ID") },
                            placeholder = { Text("例如：1:123456789:android:abcdef") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = firebaseApiKey,
                            onValueChange = {
                                firebaseApiKey = it
                                validationError = null
                            },
                            label = { Text("Firebase Web API Key") },
                            placeholder = { Text("例如：AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                validationError?.let { errorText ->
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 操作按鈕列
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (validateInputs()) {
                                onSyncNow(
                                    syncEnabled,
                                    syncAccountId,
                                    firebaseProjectId,
                                    firebaseAppId,
                                    firebaseApiKey
                                )
                            }
                        },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
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
                            onClick = {
                                if (validateInputs()) {
                                    onSaveConfig(
                                        syncEnabled,
                                        syncAccountId,
                                        firebaseProjectId,
                                        firebaseAppId,
                                        firebaseApiKey
                                    )
                                }
                            }
                        ) {
                            Text("儲存設定")
                        }
                    }
                }
            }
        }
    }
}
