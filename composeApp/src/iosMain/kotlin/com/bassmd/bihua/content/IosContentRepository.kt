@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * Read-only content for iOS, exported from the Android content database by
 * `scripts/export-ios-content.py`. One mode is parsed at a time and cached, so
 * switching scripts does not keep four full dictionaries in memory.
 */
class IosContentRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<StudyMode, LoadedContent>()

    suspend fun characters(mode: StudyMode): List<CharacterEntry> =
        withContext(Dispatchers.Default) { load(mode).characters }

    suspend fun find(mode: StudyMode, id: String): CharacterEntry? =
        withContext(Dispatchers.Default) { load(mode).characters.firstOrNull { it.id == id } }

    suspend fun searchWords(
        mode: StudyMode,
        query: String,
        limit: Int = 20,
        includeMature: Boolean = false,
    ): List<WordContext> = withContext(Dispatchers.Default) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return@withContext emptyList()
        load(mode).words
            .asSequence()
            .filter { includeMature || !it.mature }
            .filter {
                it.word.contains(needle, ignoreCase = true) ||
                    it.reading.contains(needle, ignoreCase = true) ||
                    it.meaning.contains(needle, ignoreCase = true)
            }
            .take(limit)
            .map { word ->
                WordContext(
                    word = word.word,
                    reading = word.reading,
                    meaning = word.meaning,
                    level = word.level ?: 1,
                    mature = word.mature,
                )
            }
            .toList()
    }

    suspend fun featuredWord(
        mode: StudyMode,
        epochDay: Int,
        includeMature: Boolean = false,
    ): WordContext? = withContext(Dispatchers.Default) {
        val words = load(mode).words
            .filter { !it.mature || includeMature }
            .filter { it.level != null && it.level <= 2 }
        if (words.isEmpty()) return@withContext null
        val index = ((epochDay % words.size) + words.size) % words.size
        val word = words[index]
        WordContext(
            word = word.word,
            reading = word.reading,
            meaning = word.meaning,
            level = word.level ?: 1,
            mature = word.mature,
        )
    }

    private fun load(mode: StudyMode): LoadedContent = cache.getOrPut(mode) { read(mode) }

    private fun read(mode: StudyMode): LoadedContent {
        val name = "content_${mode.name.lowercase()}"
        val path = NSBundle.mainBundle.pathForResource(name, ofType = "json", inDirectory = "Content")
            ?: NSBundle.mainBundle.pathForResource(name, ofType = "json")
            ?: error("Missing iOS content resource $name.json")
        val text = NSString.stringWithContentsOfFile(
            path = path,
            encoding = NSUTF8StringEncoding,
            error = null,
        ) ?: error("Could not read $path")
        val file = json.decodeFromString(ContentFile.serializer(), text)
        val wordsByCharacter = mutableMapOf<String, MutableList<WordContext>>()
        val words = file.words.map { word ->
            val context = WordContext(
                word = word.word,
                reading = word.reading,
                meaning = word.meaning,
                level = word.level ?: 1,
                mature = word.mature,
            )
            word.characters.forEach { characterId ->
                wordsByCharacter.getOrPut(characterId) { mutableListOf() }.add(context)
            }
            word
        }
        val characters = file.characters.map { entry ->
            CharacterEntry(
                id = entry.id,
                character = entry.character,
                reading = entry.reading,
                readingAlt = entry.readingAlt,
                meaning = entry.meaning,
                traditional = entry.traditional,
                simplified = entry.simplified,
                mode = mode,
                level = entry.level,
                levelLabel = entry.levelLabel,
                status = StudyStatus.NEW,
                writingSet = entry.writingSet,
                words = wordsByCharacter[entry.id].orEmpty(),
                glyphGroupId = entry.glyphGroupId,
                strokeCount = entry.strokeCount,
                strokeSource = "",
                frequency = entry.frequency,
            )
        }
        return LoadedContent(
            characters = characters,
            words = words,
            meta = file.meta.mapValues { (_, value) ->
                CharacterMeta(
                    radical = value.radical,
                    decomposition = value.decomposition,
                    etymology = value.etymology,
                    etymologyType = value.etymologyType,
                    phonetic = value.phonetic,
                    semantic = value.semantic,
                )
            },
        )
    }
}

private data class LoadedContent(
    val characters: List<CharacterEntry>,
    val words: List<ContentWord>,
    val meta: Map<String, CharacterMeta>,
)

@Serializable
private data class ContentFile(
    val mode: String,
    val characters: List<ContentCharacter>,
    val words: List<ContentWord>,
    val meta: Map<String, ContentMeta> = emptyMap(),
)

@Serializable
private data class ContentCharacter(
    val id: String,
    val glyphGroupId: String,
    val character: String,
    val level: Int,
    val levelLabel: String,
    val listIndex: Int = 0,
    val reading: String,
    val readingAlt: String = "",
    val meaning: String,
    val traditional: String = "",
    val simplified: String = "",
    val writingSet: Boolean = false,
    val frequency: Int? = null,
    val strokeCount: Int = 0,
)

@Serializable
private data class ContentWord(
    val id: Int,
    val word: String,
    val reading: String,
    val meaning: String,
    val level: Int? = null,
    val common: Boolean = false,
    val mature: Boolean = false,
    val frequency: Int? = null,
    val characters: List<String> = emptyList(),
)

@Serializable
private data class ContentMeta(
    val radical: String = "",
    val decomposition: String = "",
    val etymology: String = "",
    val etymologyType: String = "",
    val phonetic: String = "",
    val semantic: String = "",
)
