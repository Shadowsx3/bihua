package com.bassmd.bihua.practice

import com.bassmd.bihua.spellbook.SpellFamilies

/**
 * The first word of a gloss, for the word on the scroll: "horse; to get on" reads
 * "horse". Blank glosses return null so the caller can fall back to the family name.
 */
internal fun shortGloss(meaning: String?): String? {
    val word = meaning
        ?.substringBefore(';')
        ?.substringBefore(',')
        ?.trim()
        .orEmpty()
    return word.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() }
}

/** Emoji for a spell: an animal's own face when it is one, otherwise its element. */
private val AnimalEmoji = mapOf(
    "马" to "🐴", "虎" to "🐯", "兔" to "🐰", "龟" to "🐢", "蛙" to "🐸",
    "蜂" to "🐝", "鸟" to "🐦", "鱼" to "🐟", "龙" to "🐉", "猫" to "🐱",
    "狗" to "🐶", "牛" to "🐮", "羊" to "🐑", "猪" to "🐷", "蛇" to "🐍",
    // Traditional, Japanese, and Korean forms of the same animals.
    "馬" to "🐴", "鳥" to "🐦", "魚" to "🐟", "龍" to "🐉", "貓" to "🐱",
    "兎" to "🐰", "龜" to "🐢", "亀" to "🐢", "蟲" to "🐛",
    "鷄" to "🐔", "鶏" to "🐔", "鴨" to "🦆", "豬" to "🐷", "熊" to "🐻",
    "鹿" to "🦌", "狐" to "🦊", "鼠" to "🐭", "象" to "🐘", "獅" to "🦁",
    "狼" to "🐺", "猴" to "🐵", "鵝" to "🦢", "鶴" to "🦢", "鷹" to "🦅",
    "燕" to "🐦", "蟻" to "🐜", "蛛" to "🕷️", "鯨" to "🐋", "鯊" to "🦈",
    "猿" to "🐒", "豹" to "🐆", "驢" to "🐴", "駱" to "🐫", "駝" to "🐫",
)

internal fun spellEmoji(character: String, family: String): String {
    AnimalEmoji[character]?.let { return it }
    return when (family) {
        "water", "boat" -> "💧"
        "ice", "snow" -> "❄️"
        "fire" -> "🔥"
        "lightning" -> "⚡"
        "sun", "light" -> "☀️"
        "moon", "dream", "shadow" -> "🌙"
        "cloud" -> "☁️"
        "wind" -> "🌬️"
        "rain" -> "🌧️"
        "earth", "stone" -> "🪨"
        "mountain" -> "⛰️"
        "metal" -> "⚙️"
        "wood", "leaf" -> "🍃"
        "hand" -> "✋"
        "boat" -> "⛵"
        "tool" -> "🔧"
        "rune" -> "🔮"
        "mirror" -> "🪞"
        "key" -> "🔑"
        "potion" -> "🧪"
        "illusion" -> "🎭"
        "oracle" -> "👁️"
        "spirit", "soul" -> "👻"
        "ward" -> "🛡️"
        else -> "✨"
    }
}

/** The word on the scroll after a cast: the first gloss, or the family name. */
internal fun badgeWord(meaning: String?, family: String): String =
    shortGloss(meaning) ?: SpellFamilies.family(family)?.name ?: "Spell"

/** The gloss in the cast card: the current course's meaning, then the scene fallback. */
internal fun definitionGloss(
    character: String,
    characters: List<com.bassmd.bihua.content.CharacterEntry>,
    fallback: String,
): String = characters.firstOrNull { it.character == character }?.meaning ?: fallback

/** The family shown on the result card: a book pick, the cast, then the scene spell. */
internal fun activeFamily(picked: String?, cast: String, scene: String): String =
    picked ?: cast.ifEmpty { scene }
