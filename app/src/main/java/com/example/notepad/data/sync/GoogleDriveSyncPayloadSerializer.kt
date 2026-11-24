package com.example.notepad.data.sync

import com.example.notepad.data.local.entity.SyncEntityType
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer

/**
 * Google 雲端硬碟同步快照 JSON 序列化與反序列化工具
 *
 * 使用顯式 [JsonReader] 與 [JsonWriter] 處理序列化與型別還原，
 * 避免 R8 混淆或反射限制造成欄位遺失，並完整保留 64 位元整數（時間戳與色彩值）。
 */
object GoogleDriveSyncPayloadSerializer {

    private const val SNAPSHOT_VERSION = 1

    /**
     * 將多筆 [CloudSyncDocument] 打包為單一 Google Drive 快照 JSON 字串
     */
    fun serializeSnapshot(
        documents: List<CloudSyncDocument>,
        snapshotUpdatedAt: Long = System.currentTimeMillis()
    ): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.indent = "  "
            writer.beginObject()
            writer.name("version").value(SNAPSHOT_VERSION)
            writer.name("updatedAt").value(snapshotUpdatedAt)
            writer.name("documents")
            writer.beginArray()
            documents.forEach { doc ->
                writeDocument(writer, doc)
            }
            writer.endArray()
            writer.endObject()
        }
        return buffer.readUtf8()
    }

    /**
     * 從 Google Drive 快照 JSON 字串還原所有 [CloudSyncDocument]
     */
    fun deserializeSnapshot(json: String): List<CloudSyncDocument> {
        if (json.isBlank()) return emptyList()
        val documents = mutableListOf<CloudSyncDocument>()
        val buffer = Buffer().writeUtf8(json)
        JsonReader.of(buffer).use { reader ->
            if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
                return emptyList()
            }
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "documents" -> {
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                readDocument(reader)?.let { documents.add(it) }
                            }
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return documents
    }

    private fun writeDocument(writer: JsonWriter, document: CloudSyncDocument) {
        writer.beginObject()
        writer.name("entityType").value(document.entityType.value)
        writer.name("syncId").value(document.syncId)
        writer.name("updatedAt").value(document.updatedAt)
        writer.name("isDeleted").value(document.isDeleted)
        writer.name("payload")
        writeAnyMap(writer, document.payload)
        writer.endObject()
    }

    private fun writeAnyMap(writer: JsonWriter, map: Map<String, Any?>) {
        writer.beginObject()
        map.forEach { (key, value) ->
            writer.name(key)
            writeAnyValue(writer, value)
        }
        writer.endObject()
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeAnyValue(writer: JsonWriter, value: Any?) {
        when (value) {
            null -> writer.nullValue()
            is Boolean -> writer.value(value)
            is Int -> writer.value(value.toLong())
            is Long -> writer.value(value)
            is Float -> writer.value(value.toDouble())
            is Double -> writer.value(value)
            is Number -> writer.value(value.toLong())
            is String -> writer.value(value)
            is Map<*, *> -> writeAnyMap(writer, value as Map<String, Any?>)
            is Iterable<*> -> {
                writer.beginArray()
                value.forEach { item -> writeAnyValue(writer, item) }
                writer.endArray()
            }

            else -> writer.value(value.toString())
        }
    }

    private fun readDocument(reader: JsonReader): CloudSyncDocument? {
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        var entityTypeRaw: String? = null
        var syncId: String? = null
        var updatedAt = 0L
        var isDeleted = false
        var payload: Map<String, Any?> = emptyMap()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "entityType" -> entityTypeRaw = readNullableString(reader)
                "syncId" -> syncId = readNullableString(reader)
                "updatedAt" -> updatedAt = readLong(reader) ?: 0L
                "isDeleted" -> isDeleted = readBoolean(reader) ?: false
                "payload" -> payload = readAnyMap(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val entityType = entityTypeRaw?.let { SyncEntityType.fromValue(it) } ?: return null
        val validSyncId = syncId?.takeIf { it.isNotBlank() } ?: return null
        return CloudSyncDocument(
            entityType = entityType,
            syncId = validSyncId,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            payload = payload
        )
    }

    private fun readAnyMap(reader: JsonReader): Map<String, Any?> {
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return emptyMap()
        }
        val map = linkedMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            map[key] = readAnyValue(reader)
        }
        reader.endObject()
        return map
    }

    private fun readAnyValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                null
            }

            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> readNumber(reader)
            JsonReader.Token.BEGIN_OBJECT -> readAnyMap(reader)
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readAnyValue(reader))
                }
                reader.endArray()
                list
            }

            else -> {
                reader.skipValue()
                null
            }
        }
    }

    /**
     * 以字串讀取 JSON 數值並優先解析為 [Long]，避免 [Double] 失真
     */
    private fun readNumber(reader: JsonReader): Number {
        val raw = reader.nextString()
        return raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: 0L
    }

    private fun readNullableString(reader: JsonReader): String? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            null
        } else {
            reader.nextString()
        }
    }

    private fun readLong(reader: JsonReader): Long? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                null
            }

            JsonReader.Token.NUMBER -> readNumber(reader).toLong()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun readBoolean(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                null
            }

            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}
