package com.example.notepad.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 專供單元測試使用的 Windows 相容 DataStore<Preferences> 工廠。
 *
 * 解決 `androidx.datastore:datastore-core-jvm` 內建 `FileStorage` 在 Windows NTFS 下
 * 第二次寫入同一檔案時呼叫 `File.renameTo` 因目標檔已存在而拋出 `IOException: Unable to rename` 的問題，
 * 同時完整保留「以檔案路徑隔離」與「跨實例模擬重新啟動持久化」之測試語意。
 */
object TestPreferenceDataStore {

    private class FileBackedState {
        val mutex = Mutex()
        val stateFlow = MutableStateFlow(emptyPreferences())
    }

    private val storesByPath = ConcurrentHashMap<String, FileBackedState>()

    @Suppress("UNUSED_PARAMETER")
    fun create(
        scope: CoroutineScope? = null,
        produceFile: () -> File
    ): DataStore<Preferences> {
        return object : DataStore<Preferences> {
            private val backing: FileBackedState
                get() {
                    val path = produceFile().absolutePath
                    return storesByPath.computeIfAbsent(path) { FileBackedState() }
                }

            override val data: Flow<Preferences>
                get() = backing.stateFlow.asStateFlow()

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences {
                val target = backing
                return target.mutex.withLock {
                    val updated = transform(target.stateFlow.value).toPreferences()
                    target.stateFlow.value = updated
                    updated
                }
            }
        }
    }
}
