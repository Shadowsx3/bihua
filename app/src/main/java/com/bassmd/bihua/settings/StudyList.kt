package com.bassmd.bihua.settings

import java.util.UUID
import kotlinx.serialization.Serializable

/** A named collection of characters and words, in the order the learner added them. */
@Serializable
data class StudyList(
    val id: String,
    val name: String,
    val characterIds: List<String> = emptyList(),
    val words: List<String> = emptyList(),
    /** manual, preset, imported */
    val source: String = "manual",
    val createdAtMs: Long = 0L,
) {
    val size: Int get() = characterIds.size + words.size
    val hasCharacters: Boolean get() = characterIds.isNotEmpty()
    val hasWords: Boolean get() = words.isNotEmpty()
}

/**
 * Pure helpers for named study lists. A list is a collection, not a filter: it
 * never changes the review queue by itself. Studying a list starts a session
 * from its characters; practising its words opens free word practice.
 */
object StudyLists {
    const val LEGACY_ID = "legacy-my-list"
    const val MAX_NAME = 40
    const val DEFAULT_NAME = "My list"

    fun newId(): String = "list-${UUID.randomUUID()}"

    fun active(settings: AppSettings): StudyList? =
        settings.studyLists.firstOrNull { it.id == settings.activeStudyListId }

    /** Turn a pre-1.0.8 flat list into the first named list, once. */
    fun ensureMigrated(settings: AppSettings): AppSettings {
        var next = settings
        if (next.studyLists.isEmpty() && next.studyList.isNotEmpty()) {
            val list = StudyList(
                id = LEGACY_ID,
                name = DEFAULT_NAME,
                characterIds = next.studyList.distinct(),
                source = "manual",
                createdAtMs = 0L,
            )
            next = next.copy(studyLists = listOf(list), activeStudyListId = list.id)
        }
        val activeId = next.activeStudyListId
        if (activeId != null && next.studyLists.none { it.id == activeId }) {
            next = next.copy(activeStudyListId = null, studyList = emptyList())
        }
        val active = active(next)
        if (active != null && next.studyList != active.characterIds) {
            next = next.copy(studyList = active.characterIds)
        }
        return next
    }

    /** Point "Add to list" at this list. Review is untouched. */
    fun activate(settings: AppSettings, id: String?): AppSettings {
        val list = settings.studyLists.firstOrNull { it.id == id }
        return if (list == null) {
            settings.copy(studyList = emptyList(), activeStudyListId = null)
        } else {
            settings.copy(studyList = list.characterIds, activeStudyListId = list.id)
        }
    }

    fun add(settings: AppSettings, list: StudyList, activate: Boolean = false): AppSettings {
        val clean = list.copy(
            name = uniqueName(settings, list.name),
            characterIds = list.characterIds.distinct(),
            words = list.words.distinct(),
        )
        val next = settings.copy(studyLists = settings.studyLists + clean)
        return if (activate) activate(next, clean.id) else next
    }

    fun replace(settings: AppSettings, list: StudyList): AppSettings {
        val lists = settings.studyLists.map { if (it.id == list.id) list else it }
        val next = settings.copy(studyLists = lists)
        return if (settings.activeStudyListId == list.id) {
            next.copy(studyList = list.characterIds.distinct())
        } else {
            next
        }
    }

    fun remove(settings: AppSettings, id: String): AppSettings {
        val wasActive = settings.activeStudyListId == id
        return settings.copy(
            studyLists = settings.studyLists.filterNot { it.id == id },
            activeStudyListId = if (wasActive) null else settings.activeStudyListId,
            studyList = if (wasActive) emptyList() else settings.studyList,
        )
    }

    fun rename(settings: AppSettings, id: String, name: String): AppSettings {
        val current = settings.studyLists.firstOrNull { it.id == id } ?: return settings
        val clean = name.trim().take(MAX_NAME).ifBlank { DEFAULT_NAME }
        val others = settings.copy(studyLists = settings.studyLists.filterNot { it.id == id })
        return replace(settings, current.copy(name = uniqueName(others, clean)))
    }

    fun duplicate(settings: AppSettings, id: String): AppSettings? {
        val source = settings.studyLists.firstOrNull { it.id == id } ?: return null
        val copy = source.copy(
            id = newId(),
            name = uniqueName(settings, "${source.name} copy"),
            createdAtMs = System.currentTimeMillis(),
        )
        return add(settings, copy)
    }

    fun clear(settings: AppSettings, id: String): AppSettings {
        val list = settings.studyLists.firstOrNull { it.id == id } ?: return settings
        return replace(settings, list.copy(characterIds = emptyList(), words = emptyList()))
    }

    /** Toggle a character in the active list, creating "My list" when needed. */
    fun toggleCharacter(settings: AppSettings, characterId: String): AppSettings {
        val list = active(settings)
        if (list != null) {
            val ids = if (characterId in list.characterIds) {
                list.characterIds - characterId
            } else {
                list.characterIds + characterId
            }
            return replace(settings, list.copy(characterIds = ids))
        }
        val created = StudyList(
            id = newId(),
            name = uniqueName(settings, DEFAULT_NAME),
            characterIds = listOf(characterId),
            createdAtMs = System.currentTimeMillis(),
        )
        return add(settings, created, activate = true)
    }

    /** Toggle a word in the active list, creating "My words" when needed. */
    fun toggleWord(settings: AppSettings, word: String): AppSettings {
        val text = word.trim()
        if (text.isEmpty()) return settings
        val list = active(settings)
        if (list != null) {
            val words = if (text in list.words) list.words - text else list.words + text
            return replace(settings, list.copy(words = words))
        }
        val created = StudyList(
            id = newId(),
            name = uniqueName(settings, "My words"),
            words = listOf(text),
            createdAtMs = System.currentTimeMillis(),
        )
        return add(settings, created, activate = true)
    }

    fun containsWord(settings: AppSettings, word: String): Boolean =
        active(settings)?.words?.contains(word.trim()) == true

    fun uniqueName(settings: AppSettings, wanted: String): String {
        val base = wanted.trim().take(MAX_NAME).ifBlank { DEFAULT_NAME }
        if (settings.studyLists.none { it.name.equals(base, ignoreCase = true) }) return base
        var index = 2
        while (true) {
            val suffix = " ($index)"
            // Keep the suffix intact. Truncating the combined name at MAX_NAME
            // would collapse long names back to the same string.
            val candidate = base.take(MAX_NAME - suffix.length).trimEnd() + suffix
            if (settings.studyLists.none { it.name.equals(candidate, ignoreCase = true) }) {
                return candidate
            }
            index++
        }
    }
}
