package com.bassmd.bihua.learn

import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.RecallCue

data class RecallCuePrompt(
    val label: String,
    val text: String,
)

/**
 * Builds the small prompt shown before a recall attempt. It must never include
 * the character itself: the learner is writing it from memory. A word cue
 * therefore shows the word's reading and meaning, not the word's characters.
 */
object RecallPrompt {

    fun choose(setting: RecallCue, seed: Int): RecallCue =
        if (setting == RecallCue.MIXED) {
            val options = listOf(RecallCue.MEANING, RecallCue.READING, RecallCue.WORD)
            options[Math.floorMod(seed, options.size)]
        } else {
            setting
        }

    fun build(
        cue: RecallCue,
        reading: String,
        meaning: String,
        wordReading: String?,
        wordMeaning: String?,
        mode: StudyMode,
        pinyinNumbers: Boolean,
    ): RecallCuePrompt = when (cue) {
        RecallCue.READING -> RecallCuePrompt(
            label = "Write the character read",
            text = Pinyin.display(reading, mode, pinyinNumbers),
        )
        RecallCue.WORD -> {
            if (!wordReading.isNullOrBlank() && !wordMeaning.isNullOrBlank()) {
                RecallCuePrompt(
                    label = "Write the character in the word",
                    text = "${Pinyin.display(wordReading, mode, pinyinNumbers)} · $wordMeaning",
                )
            } else {
                RecallCuePrompt(
                    label = "Write the character that means",
                    text = meaning,
                )
            }
        }
        RecallCue.MEANING, RecallCue.MIXED -> RecallCuePrompt(
            label = "Write the character that means",
            text = meaning,
        )
    }
}
