package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyListsTest {

    private fun settings() = AppSettings()

    @Test
    fun legacyFlatListBecomesOneNamedList() {
        val migrated = StudyLists.ensureMigrated(
            settings().copy(studyList = listOf("a", "b", "a")),
        )
        assertEquals(1, migrated.studyLists.size)
        assertEquals(listOf("a", "b"), migrated.studyLists.first().characterIds)
        assertEquals(StudyLists.LEGACY_ID, migrated.activeStudyListId)
        assertEquals(listOf("a", "b"), migrated.studyList)
    }

    @Test
    fun migrationIsStable() {
        val once = StudyLists.ensureMigrated(settings().copy(studyList = listOf("a")))
        val twice = StudyLists.ensureMigrated(once)
        assertEquals(once.studyLists, twice.studyLists)
        assertEquals(once.activeStudyListId, twice.activeStudyListId)
    }

    @Test
    fun activateMirrorsCharactersWithoutTouchingOtherLists() {
        val list = StudyList(id = "one", name = "One", characterIds = listOf("a", "b"))
        val next = StudyLists.add(settings(), list, activate = true)
        assertEquals(list.characterIds, next.studyList)
        assertEquals("one", next.activeStudyListId)

        val cleared = StudyLists.activate(next, null)
        assertTrue(cleared.studyList.isEmpty())
        assertNull(cleared.activeStudyListId)
        assertEquals(1, cleared.studyLists.size)
    }

    @Test
    fun toggleCharacterCreatesAndUpdatesActiveList() {
        val created = StudyLists.toggleCharacter(settings(), "a")
        assertEquals(1, created.studyLists.size)
        assertEquals(listOf("a"), created.studyList)
        assertEquals(StudyLists.DEFAULT_NAME, created.studyLists.first().name)

        val removed = StudyLists.toggleCharacter(created, "a")
        assertTrue(removed.studyList.isEmpty())
        assertTrue(removed.studyLists.first().characterIds.isEmpty())
    }

    @Test
    fun wordToggleUsesTheActiveWordList() {
        val created = StudyLists.toggleWord(settings(), "你好")
        assertEquals(listOf("你好"), created.studyLists.first().words)
        assertTrue(StudyLists.containsWord(created, "你好"))

        val removed = StudyLists.toggleWord(created, "你好")
        assertTrue(removed.studyLists.first().words.isEmpty())
        assertFalse(StudyLists.containsWord(removed, "你好"))
    }

    @Test
    fun wordsJoinTheActiveListWithoutCreatingAnother() {
        val chars = StudyLists.toggleCharacter(settings(), "a")
        val withWord = StudyLists.toggleWord(chars, "你好")
        assertEquals(1, withWord.studyLists.size)
        assertEquals(listOf("a"), withWord.studyLists.first().characterIds)
        assertEquals(listOf("你好"), withWord.studyLists.first().words)
        assertEquals(StudyLists.DEFAULT_NAME, StudyLists.active(withWord)?.name)
    }

    @Test
    fun removeActiveListClearsTheMirror() {
        val added = StudyLists.add(
            settings(),
            StudyList(id = "one", name = "One", characterIds = listOf("a")),
            activate = true,
        )
        val removed = StudyLists.remove(added, "one")
        assertTrue(removed.studyLists.isEmpty())
        assertTrue(removed.studyList.isEmpty())
        assertNull(removed.activeStudyListId)
    }

    @Test
    fun renameKeepsNamesUniqueAndBounded() {
        val first = StudyLists.add(settings(), StudyList(id = "a", name = "Words"))
        val second = StudyLists.add(first, StudyList(id = "b", name = "Other"))
        val renamed = StudyLists.rename(second, "b", "Words")
        assertEquals("Words (2)", renamed.studyLists.last().name)

        val long = StudyLists.rename(renamed, "b", "x".repeat(120))
        assertEquals(StudyLists.MAX_NAME, long.studyLists.last().name.length)
    }

    @Test
    fun duplicateCopiesCharactersAndWords() {
        val source = StudyLists.add(
            settings(),
            StudyList(id = "a", name = "Mixed", characterIds = listOf("a"), words = listOf("你好")),
        )
        val copied = StudyLists.duplicate(source, "a")!!
        assertEquals(2, copied.studyLists.size)
        assertEquals(listOf("a"), copied.studyLists.last().characterIds)
        assertEquals(listOf("你好"), copied.studyLists.last().words)
        assertTrue(copied.studyLists.last().name.startsWith("Mixed copy"))
    }

    @Test
    fun clearKeepsTheShelf() {
        val added = StudyLists.add(
            settings(),
            StudyList(id = "a", name = "Mixed", characterIds = listOf("a"), words = listOf("你好")),
            activate = true,
        )
        val cleared = StudyLists.clear(added, "a")
        assertEquals(1, cleared.studyLists.size)
        assertTrue(cleared.studyLists.first().characterIds.isEmpty())
        assertTrue(cleared.studyLists.first().words.isEmpty())
        assertTrue(cleared.studyList.isEmpty())
    }
}
