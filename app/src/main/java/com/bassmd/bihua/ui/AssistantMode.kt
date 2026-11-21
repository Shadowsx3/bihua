package com.bassmd.bihua.ui

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode

enum class AssistantMode(val label: String) {
    GENERAL("General"),
    MNEMONIC("Mnemonic"),
    PARTS("Parts"),
    HISTORY("History"),
    SOUND("Sound"),
    CUSTOM("Custom"),
}

data class AssistantRequest(val characterId: String, val mode: AssistantMode)

/**
 * The starting message a mode drops into the composer. The learner can edit it
 * before sending; nothing here is submitted on its own.
 */
fun promptFor(mode: AssistantMode, character: CharacterEntry?): String {
    if (mode == AssistantMode.GENERAL || character == null) return ""
    return when (mode) {
        AssistantMode.GENERAL -> ""
        AssistantMode.MNEMONIC ->
            "Give me one short mnemonic for ${character.character} " +
                "(read ${character.reading}, meaning ${character.meaning}). " +
                "Build it from the shape, then link it to the meaning. Two sentences at most."
        AssistantMode.PARTS ->
            "Break ${character.character} into the parts you can actually see. " +
                "Name each part, say what it seems to do here, then one line on how they fit together. " +
                "If a part is unclear, say so instead of inventing one."
        AssistantMode.HISTORY ->
            "Tell me where ${character.character} comes from. " +
                "Only give dates or source forms you are confident about. " +
                "If the history is uncertain, say that plainly and give the most likely story instead."
        AssistantMode.SOUND ->
            "Help me remember that ${character.character} reads ${character.reading} and means ${character.meaning}. " +
                "Give one sound hook or tiny scene I can picture while writing it. Keep it short."
        AssistantMode.CUSTOM -> ""
    }
}

private fun StudyMode.plainName(): String = when (this) {
    StudyMode.SIMPLIFIED -> "Simplified Chinese"
    StudyMode.TRADITIONAL -> "Traditional Chinese"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}

/**
 * The instruction the model receives. It is deliberately narrow: this is a
 * writing tutor for one learner, not a general chatbot.
 */
fun systemPrompt(character: CharacterEntry?, mode: StudyMode): String = buildString {
    append("You are Bihua, a patient handwriting tutor for one adult learner ")
    append("studying ${mode.plainName()} characters with a stylus.")
    append(" The learner writes each character by hand; you only help them remember it.")
    append(" Rules: ")
    append("Be concrete and short. Aim for 40 to 90 words unless asked for more. ")
    append("Prefer plain words and short sentences. No lists longer than four items. ")
    append("Do not open with praise or filler such as \"Great question\" or \"Certainly\". ")
    append("Do not use the pattern \"It is not X, it is Y\". ")
    append("Never invent etymology, readings, stroke counts, or component names. ")
    append("If you are unsure, say what is uncertain in one clause and move on. ")
    append("Never grade the learner, never suggest a review date, and never claim to see their ink.")
    if (character != null) {
        append(" The current character is ${character.character} ")
        append("(${character.mode.plainName()} form). ")
        append("Reading: ${character.reading.ifBlank { "unknown" }}. ")
        append("Meaning: ${character.meaning.ifBlank { "unknown" }}. ")
        if (character.strokeCount > 0) append("Stroke count: ${character.strokeCount}. ")
        append("Answer about this character only, and treat the reading and meaning above as correct.")
    } else {
        append(" No character is selected, so answer as a general writing and study coach. ")
        append("Do not assume a specific character, and do not ask which one unless it changes your answer.")
    }
}
