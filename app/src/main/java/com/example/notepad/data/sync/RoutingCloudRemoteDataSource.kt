package com.example.notepad.data.sync

import com.example.notepad.data.preferences.CloudBackendProvider
import com.example.notepad.data.preferences.SyncSettings

/**
 * 依使用者選擇的雲端服務提供者（預設 Google 雲端硬碟，或切換為備援 Firebase Firestore）
 * 自動分派實際讀寫之 [CloudRemoteDataSource] 轉接器
 */
class RoutingCloudRemoteDataSource(
    private val googleDriveDataSource: CloudRemoteDataSource,
    private val firebaseDataSource: CloudRemoteDataSource
) : CloudRemoteDataSource {

    private fun delegateFor(settings: SyncSettings): CloudRemoteDataSource {
        return when (settings.cloudProvider) {
            CloudBackendProvider.GOOGLE_DRIVE -> googleDriveDataSource
            CloudBackendProvider.FIREBASE -> firebaseDataSource
        }
    }

    override fun isConfigured(settings: SyncSettings): Boolean {
        return delegateFor(settings).isConfigured(settings)
    }

    override suspend fun fetchAllDocuments(
        accountId: String,
        settings: SyncSettings
    ): List<CloudSyncDocument> {
        return delegateFor(settings).fetchAllDocuments(accountId, settings)
    }

    override suspend fun upsertDocuments(
        accountId: String,
        settings: SyncSettings,
        documents: List<CloudSyncDocument>
    ) {
        delegateFor(settings).upsertDocuments(accountId, settings, documents)
    }
}
