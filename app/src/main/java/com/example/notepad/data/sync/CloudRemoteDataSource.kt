package com.example.notepad.data.sync

import com.example.notepad.data.preferences.SyncSettings

/**
 * 雲端資料遠端存取抽象介面（隔離 Firebase Firestore 實體依賴以利測試）
 */
interface CloudRemoteDataSource {
    /**
     * 檢查遠端服務（例如 google-services.json 或手動連線參數）是否已就緒
     */
    fun isConfigured(settings: SyncSettings): Boolean

    /**
     * 自遠端空間下載所有同步文件（包含活躍項目與遠端刪除標記）
     */
    suspend fun fetchAllDocuments(accountId: String, settings: SyncSettings): List<CloudSyncDocument>

    /**
     * 批次上傳新增／更新／刪除標記文件至遠端空間
     */
    suspend fun upsertDocuments(
        accountId: String,
        settings: SyncSettings,
        documents: List<CloudSyncDocument>
    )
}
