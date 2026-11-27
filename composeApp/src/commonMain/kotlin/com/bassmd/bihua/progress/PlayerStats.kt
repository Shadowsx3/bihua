package com.bassmd.bihua.progress

data class PlayerStats(
    val totalXp: Int,
    val level: Int,
    val title: String,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
) {
    val progress: Float get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)

    companion object {
        fun forXp(totalXp: Int): PlayerStats {
            var level = 1
            while (level < 99 && totalXp >= xpForLevel(level + 1)) level++
            val base = xpForLevel(level)
            val next = xpForLevel(level + 1)
            return PlayerStats(
                totalXp = totalXp,
                level = level,
                title = titleFor(level),
                xpIntoLevel = totalXp - base,
                xpForNextLevel = (next - base).coerceAtLeast(1),
            )
        }

        fun xpForLevel(level: Int): Int = 25 * (level - 1) * (level + 2)

        fun titleFor(level: Int): String = when {
            level >= 15 -> "Master of Strokes"
            level >= 10 -> "Calligrapher"
            level >= 7 -> "Ink Adept"
            level >= 5 -> "Stroke Keeper"
            level >= 3 -> "Brushhand"
            else -> "Inkling"
        }
    }
}
