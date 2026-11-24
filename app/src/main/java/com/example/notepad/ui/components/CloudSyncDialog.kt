package com.example.notepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LinkOff
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
import com.example.notepad.data.preferences.CloudBackendProvider
import com.example.notepad.data.sync.CloudSyncUiState
import com.example.notepad.ui.theme.LocalCustomButtonColors
import java.util.UUID

/**
 * 雲端資料同步設定與狀態管理對話框（以 Google 雲端硬碟為主介面，並支援進階切換 Firebase 備援）
 *
 * 顯示網路連線狀態、Google 帳號綁定狀態、待同步異動筆數與最後同步時間，
 * 提供一鍵授權 Google 雲端硬碟、切換帳號、中斷連結、儲存設定與「立即雙向同步」操作。
 */
@Composable
fun CloudSyncDialog(
    uiState: CloudSyncUiState,
    onAuthorizeGoogleDrive: () -> Unit,
    onDisconnectGoogleDrive: () -> Unit,
    isAuthorizingGoogle: Boolean = false,
    googleAuthError: String? = null,
    onSaveConfig: (
        cloudProvider: CloudBackendProvider,
        syncEnabled: Boolean,
        syncAccountId: String,
        googleDriveSpaceKey: String,
        googleDriveAccessToken: String,
        firebaseProjectId: String,
        firebaseAppId: String,
        firebaseApiKey: String
    ) -> Unit,
    onSyncNow: (
        cloudProvider: CloudBackendProvider,
        syncEnabled: Boolean,
        syncAccountId: String,
        googleDriveSpaceKey: String,
        googleDriveAccessToken: String,
        firebaseProjectId: String,
        firebaseAppId: String,
        firebaseApiKey: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialSettings = uiState.syncSettings

    var cloudProvider by remember { mutableStateOf(initialSettings.cloudProvider) }
    var syncEnabled by remember { mutableStateOf(initialSettings.syncEnabled) }
    var syncAccountId by remember { mutableStateOf(initialSettings.syncAccountId) }
    var googleDriveSpaceKey by remember { mutableStateOf(initialSettings.googleDriveSpaceKey) }
    var googleDriveAccessToken by remember { mutableStateOf(initialSettings.googleDriveAccessToken) }
    var firebaseProjectId by remember { mutableStateOf(initialSettings.firebaseProjectId) }
    var firebaseAppId by remember { mutableStateOf(initialSettings.firebaseAppId) }
    var firebaseApiKey by remember { mutableStateOf(initialSettings.firebaseApiKey) }
    var showAdvancedConfig by remember {
        mutableStateOf(
            initialSettings.cloudProvider == CloudBackendProvider.FIREBASE ||
                initialSettings.googleDriveAccessToken.isNotBlank()
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    // 若初始進入對話框時系統剛生成預設同步代碼，或剛完成 Google 授權，同步刷新對應狀態
    LaunchedEffect(uiState.syncSettings.syncAccountId) {
        if (syncAccountId.isBlank() && uiState.syncSettings.syncAccountId.isNotBlank()) {
            syncAccountId = uiState.syncSettings.syncAccountId
        }
    }
    LaunchedEffect(uiState.syncSettings.cloudProvider) {
        cloudProvider = uiState.syncSettings.cloudProvider
    }

    fun validateInputs(): Boolean {
        if (cloudProvider == CloudBackendProvider.FIREBASE) {
            val anyManualFilled = firebaseProjectId.isNotBlank() ||
                firebaseAppId.isNotBlank() ||
                firebaseApiKey.isNotBlank()
            val allManualFilled = firebaseProjectId.isNotBlank() &&
                firebaseAppId.isNotBlank() &&
                firebaseApiKey.isNotBlank()
            if (anyManualFilled && !allManualFilled) {
                validationError = "若要手動設定 Firebase，請完整填寫 Project ID、App ID 與 Web API Key（或全部留空改用 google-services.json）。"
                showAdvancedConfig = true
                showFeedback(context, validationError!!)
                return false
            }
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
                        text = if (cloudProvider == CloudBackendProvider.GOOGLE_DRIVE) {
                            "Google 雲端硬碟同步"
                        } else {
                            "雲端資料雙向同步"
                        },
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

                        val engineReady = if (cloudProvider == CloudBackendProvider.GOOGLE_DRIVE) {
                            initialSettings.hasGoogleDriveConfig()
                        } else {
                            uiState.isCloudConfigured
                        }
                        val engineLabel = if (cloudProvider == CloudBackendProvider.GOOGLE_DRIVE) {
                            if (initialSettings.googleAccountEmail.isNotBlank()) {
                                "☁️ 雲端服務：Google 雲端硬碟（${initialSettings.googleAccountEmail}）"
                            } else if (initialSettings.googleDriveAccessToken.isNotBlank()) {
                                "☁️ 雲端服務：Google 雲端硬碟（已設定手動權杖）"
                            } else {
                                "⚠️ 雲端服務：尚未連結 Google 帳號"
                            }
                        } else {
                            if (engineReady) {
                                "🔥 雲端服務：Firebase Firestore 已就緒"
                            } else {
                                "⚠️ 雲端服務：尚未設定 Firebase 專案"
                            }
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

                // === Google 雲端硬碟主要綁定操作區 ===
                if (cloudProvider == CloudBackendProvider.GOOGLE_DRIVE) {
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

                    OutlinedTextField(
                        value = googleDriveSpaceKey,
                        onValueChange = { googleDriveSpaceKey = it },
                        label = { Text("雲端硬碟備份區別代號（選填）") },
                        placeholder = { Text("留空代表同帳號所有裝置共用同步") },
                        supportingText = {
                            Text("實際存於 Google 雲端硬碟檔案：${initialSettings.copy(googleDriveSpaceKey = googleDriveSpaceKey).resolveGoogleDriveFileName()}")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // === 折疊式進階與備援設定（手動權杖／切換 Firebase）===
                OutlinedButton(
                    onClick = { showAdvancedConfig = !showAdvancedConfig },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (showAdvancedConfig) {
                            "隱藏進階設定（服務切換 / Firebase 備援）"
                        } else {
                            "進階設定（服務切換 / 手動權杖）"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showAdvancedConfig) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = showAdvancedConfig) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "選擇雲端同步後端",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CloudBackendProvider.entries.forEach { option ->
                                FilterChip(
                                    selected = cloudProvider == option,
                                    onClick = {
                                        cloudProvider = option
                                        validationError = null
                                    },
                                    label = { Text(option.label) }
                                )
                            }
                        }

                        if (cloudProvider == CloudBackendProvider.GOOGLE_DRIVE) {
                            OutlinedTextField(
                                value = googleDriveAccessToken,
                                onValueChange = { googleDriveAccessToken = it },
                                label = { Text("手動 Google Drive Access Token（開發測試選填）") },
                                placeholder = { Text("一般使用者直接點上方「連結 Google 帳號」即可") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = syncAccountId,
                                onValueChange = { syncAccountId = it },
                                label = { Text("同步空間代碼（跨裝置識別 ID）") },
                                supportingText = {
                                    Text("不同裝置填寫相同代碼即可雙向同步；留空儲存將自動產生。")
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
                                    cloudProvider,
                                    syncEnabled,
                                    syncAccountId,
                                    googleDriveSpaceKey,
                                    googleDriveAccessToken,
                                    firebaseProjectId,
                                    firebaseAppId,
                                    firebaseApiKey
                                )
                            }
                        },
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
                            onClick = {
                                if (validateInputs()) {
                                    onSaveConfig(
                                        cloudProvider,
                                        syncEnabled,
                                        syncAccountId,
                                        googleDriveSpaceKey,
                                        googleDriveAccessToken,
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
