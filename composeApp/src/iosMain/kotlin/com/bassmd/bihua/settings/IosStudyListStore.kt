@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bassmd.bihua.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.platform.AppFiles
import com.bassmd.bihua.platform.Services
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StudyListRecordFile(
    val version: Int = 1,
    val lists: List<StudyListRecord> = emptyList(),
)

/** Small, local study lists. Import/export of the Android list format follows. */
class IosStudyListStore(
    private val files: AppFiles = Services.current.files,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    var lists by mutableStateOf(load())
        private set

    init {
        if (platform.Foundation.NSProcessInfo.processInfo.arguments.contains("-reset-progress")) {
            files.delete(FILE_NAME)
            lists = emptyList()
        }
    }

    fun create(mode: StudyMode): StudyListRecord {
        val next = StudyListRecord(
            id = "list-${Clock.System.now().toEpochMilliseconds()}",
            name = "Study list ${lists.size + 1}",
            mode = mode,
            createdAtMs = Clock.System.now().toEpochMilliseconds(),
        )
        lists = lists + next
        save()
        return next
    }

    fun rename(id: String, name: String) {
        val clean = name.trim().take(60).ifBlank { "Study list" }
        lists = lists.map { if (it.id == id) it.copy(name = clean) else it }
        save()
    }

    fun delete(id: String) {
        lists = lists.filterNot { it.id == id }
        save()
    }

    fun add(listId: String, characterId: String) {
        lists = lists.map { list ->
            if (list.id == listId && characterId !in list.characters) {
                list.copy(characters = list.characters + characterId)
            } else {
                list
            }
        }
        save()
    }

    fun remove(listId: String, characterId: String) {
        lists = lists.map { list ->
            if (list.id == listId) list.copy(characters = list.characters - characterId) else list
        }
        save()
    }

    fun find(id: String): StudyListRecord? = lists.firstOrNull { it.id == id }

    private fun load(): List<StudyListRecord> {
        val bytes = runCatching { files.readBytes(FILE_NAME) }.getOrNull() ?: return emptyList()
        return runCatching {
            json.decodeFromString(StudyListRecordFile.serializer(), bytes.decodeToString()).lists
        }.getOrDefault(emptyList())
    }

    private fun save() {
        runCatching {
            files.writeBytes(
                FILE_NAME,
                json.encodeToString(StudyListRecordFile.serializer(), StudyListRecordFile(lists = lists))
                    .encodeToByteArray(),
            )
        }
    }

    private companion object {
        const val FILE_NAME = "bihua_study_lists.json"
    }
}
