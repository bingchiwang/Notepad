package com.example.notepad.data.sync

import android.content.Context
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.preferences.SyncSettings
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Firestore 雲端同步遠端資料來源實作（資料同步規則 2、4、5）
 *
 * 支援兩種連線初始化方式：
 * 1. 自動偵測專案內含 `google-services.json` 所初始化的預設 [FirebaseApp]
 * 2. 使用者於介面手動輸入 Firebase Project ID / App ID / Web API Key 動態初始化連線
 */
class FirebaseFirestoreDataSource(
    private val context: Context
) : CloudRemoteDataSource {

    companion object {
        private const val ROOT_COLLECTION = "notepad_sync"
        private const val ITEMS_SUBCOLLECTION = "items"
        private const val CUSTOM_FIREBASE_APP_NAME = "notepad_cloud_sync"
        private const val MAX_BATCH_WRITE_SIZE = 400
    }

    override fun isConfigured(settings: SyncSettings): Boolean {
        return hasDefaultFirebaseApp() || settings.hasManualFirebaseConfig()
    }

    override suspend fun fetchAllDocuments(
        accountId: String,
        settings: SyncSettings
    ): List<CloudSyncDocument> {
        val firestore = getOrInitFirestore(settings)
            ?: throw IllegalStateException("尚未設定 Firebase 專案（請新增 google-services.json 或於設定填寫專案參數）")

        val snapshot = firestore
            .collection(ROOT_COLLECTION)
            .document(accountId)
            .collection(ITEMS_SUBCOLLECTION)
            .get(Source.SERVER)
            .awaitTaskResult()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val entityType = SyncEntityType.fromValue(data["entityType"] as? String)
                ?: return@mapNotNull null
            val syncId = (data["syncId"] as? String)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
            val isDeleted = (data["isDeleted"] as? Boolean) ?: false

            @Suppress("UNCHECKED_CAST")
            val payloadMap = (data["payload"] as? Map<String, Any?>) ?: emptyMap()

            CloudSyncDocument(
                entityType = entityType,
                syncId = syncId,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                payload = payloadMap
            )
        }
    }

    override suspend fun upsertDocuments(
        accountId: String,
        settings: SyncSettings,
        documents: List<CloudSyncDocument>
    ) {
        if (documents.isEmpty()) return

        val firestore = getOrInitFirestore(settings)
            ?: throw IllegalStateException("尚未設定 Firebase 專案（請新增 google-services.json 或於設定填寫專案參數）")

        val itemsCollection = firestore
            .collection(ROOT_COLLECTION)
            .document(accountId)
            .collection(ITEMS_SUBCOLLECTION)

        // 依 Firestore WriteBatch 上限安全分批寫入
        documents.chunked(MAX_BATCH_WRITE_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { cloudDoc ->
                val docRef = itemsCollection.document(cloudDoc.documentId)
                val body = hashMapOf<String, Any?>(
                    "entityType" to cloudDoc.entityType.value,
                    "syncId" to cloudDoc.syncId,
                    "updatedAt" to cloudDoc.updatedAt,
                    "isDeleted" to cloudDoc.isDeleted,
                    "payload" to cloudDoc.payload
                )
                batch.set(docRef, body, SetOptions.merge())
            }
            batch.commit().awaitTaskResult()
        }
    }

    private fun hasDefaultFirebaseApp(): Boolean {
        return FirebaseApp.getApps(context.applicationContext).any {
            it.name == FirebaseApp.DEFAULT_APP_NAME
        }
    }

    private fun getOrInitFirestore(settings: SyncSettings): FirebaseFirestore? {
        val appContext = context.applicationContext
        val existingApps = FirebaseApp.getApps(appContext)
        val defaultApp = existingApps.firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (defaultApp != null && !settings.hasManualFirebaseConfig()) {
            return FirebaseFirestore.getInstance(defaultApp)
        }

        if (!settings.hasManualFirebaseConfig()) {
            return defaultApp?.let { FirebaseFirestore.getInstance(it) }
        }

        val customApp = existingApps.firstOrNull { it.name == CUSTOM_FIREBASE_APP_NAME }
        val needsRecreate = customApp != null && (
            customApp.options.projectId != settings.firebaseProjectId ||
                customApp.options.applicationId != settings.firebaseAppId ||
                customApp.options.apiKey != settings.firebaseApiKey
            )

        val targetApp = if (customApp == null || needsRecreate) {
            if (needsRecreate) {
                customApp?.delete()
            }
            val options = FirebaseOptions.Builder()
                .setProjectId(settings.firebaseProjectId)
                .setApplicationId(settings.firebaseAppId)
                .setApiKey(settings.firebaseApiKey)
                .build()
            FirebaseApp.initializeApp(appContext, options, CUSTOM_FIREBASE_APP_NAME)
        } else {
            customApp
        }

        return FirebaseFirestore.getInstance(targetApp)
    }

    private suspend fun <T> Task<T>.awaitTaskResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}
