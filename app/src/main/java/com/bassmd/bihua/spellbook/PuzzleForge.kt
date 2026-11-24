package com.bassmd.bihua.spellbook

import kotlin.random.Random

/** A thing in the way. It accepts some tags; a family carrying one of them solves it. */
data class Obstacle(
    val id: String,
    val prompt: String,
    val lines: List<String>,
    /** What the obstacle needs, in words. Also the nudge when a cast cannot help. */
    val need: String,
    val accepts: Set<String>,
)

data class TimeOfDay(val suffix: String, val line: String)

data class Weather(val suffix: String, val line: String)

/**
 * The authored parts the forge combines. Loaded from `puzzle_parts.db`, which is
 * built from `tools/spells/parts.sql`.
 */
class PuzzleParts(
    val familyNames: Map<String, String>,
    val familyTags: Map<String, Set<String>>,
    val obstacles: List<Obstacle>,
    /** What happens when a tag works. `{family}` is replaced with the spell's name. */
    val outcomes: Map<String, String>,
    val settings: List<String>,
    val times: List<TimeOfDay>,
    val weathers: List<Weather>,
) {
    companion object {
        /** Copies the shipped `puzzle_parts.db` out of the assets and reads it. Null if it is missing. */
        fun load(context: android.content.Context): PuzzleParts? {
            val file = java.io.File(context.filesDir, "puzzle_parts.db")
            runCatching {
                context.assets.open("puzzle_parts.db").use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!file.exists()) return null
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                file.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
            )
            return try {
                fromDatabase(db)
            } finally {
                db.close()
            }
        }

        fun fromDatabase(db: android.database.sqlite.SQLiteDatabase): PuzzleParts {
            val names = LinkedHashMap<String, String>()
            db.rawQuery("SELECT id, name FROM family ORDER BY id", null).use { c ->
                while (c.moveToNext()) names[c.getString(0)] = c.getString(1)
            }
            val tags = HashMap<String, MutableSet<String>>()
            db.rawQuery("SELECT family, tag FROM family_tag", null).use { c ->
                while (c.moveToNext()) tags.getOrPut(c.getString(0)) { mutableSetOf() }.add(c.getString(1))
            }
            val accepts = HashMap<String, MutableSet<String>>()
            db.rawQuery("SELECT obstacle_id, tag FROM obstacle_accepts", null).use { c ->
                while (c.moveToNext()) accepts.getOrPut(c.getString(0)) { mutableSetOf() }.add(c.getString(1))
            }
            val obstacles = mutableListOf<Obstacle>()
            db.rawQuery("SELECT id, prompt, lines, need FROM obstacle ORDER BY id", null).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    obstacles += Obstacle(
                        id = id,
                        prompt = c.getString(1),
                        lines = c.getString(2).split("|"),
                        need = c.getString(3),
                        accepts = accepts[id].orEmpty(),
                    )
                }
            }
            val outcomes = HashMap<String, String>()
            db.rawQuery("SELECT tag, text FROM tag_outcome", null).use { c ->
                while (c.moveToNext()) outcomes[c.getString(0)] = c.getString(1)
            }
            val settings = mutableListOf<String>()
            db.rawQuery("SELECT place FROM setting ORDER BY place", null).use { c ->
                while (c.moveToNext()) settings += c.getString(0)
            }
            val times = mutableListOf<TimeOfDay>()
            db.rawQuery("SELECT suffix, line FROM time_of_day ORDER BY name", null).use { c ->
                while (c.moveToNext()) times += TimeOfDay(c.getString(0), c.getString(1))
            }
            val weathers = mutableListOf<Weather>()
            db.rawQuery("SELECT suffix, line FROM weather ORDER BY name", null).use { c ->
                while (c.moveToNext()) weathers += Weather(c.getString(0), c.getString(1))
            }
            return PuzzleParts(names, tags, obstacles, outcomes, settings, times, weathers)
        }
    }
}

/**
 * Builds a puzzle from authored parts with a seeded random source. The same seed
 * gives the same puzzle; the combinations (obstacle, setting, time, weather, and
 * which families solve it) are far more than any library could hold.
 *
 * The rule that keeps it honest: a family solves an obstacle only when it carries
 * one of the obstacle's accepted tags. Every other family gets the obstacle's need
 * as its nudge, which names the need and never the answer.
 */
object PuzzleForge {

    fun generate(
        parts: PuzzleParts,
        random: Random,
        family: String? = null,
        previousPlace: String? = null,
    ): Puzzle {
        require(parts.obstacles.isNotEmpty()) { "no obstacles to build a puzzle from" }
        val carried = family?.let { parts.familyTags[it] }.orEmpty()
        val fitting = parts.obstacles.filter { obstacle -> obstacle.accepts.any { it in carried } }
        val obstacle = (fitting.ifEmpty { parts.obstacles }).let { it[random.nextInt(it.size)] }

        val solutions = parts.familyNames.keys
            .mapNotNull { id ->
                val matched = parts.familyTags[id].orEmpty().intersect(obstacle.accepts).sorted()
                if (matched.isEmpty()) return@mapNotNull null
                val name = parts.familyNames[id].orEmpty().lowercase()
                val outcome = parts.outcomes[matched.first()] ?: "The {family} works, and the way opens."
                Solution(families = setOf(id), text = outcome.replace("{family}", name))
            }
            .shuffled(random)
            .take(4)

        val solvedBy = solutions.flatMap { it.families }.toSet()
        val hints = parts.familyNames.keys
            .filter { it !in solvedBy }
            .associateWith { obstacle.need }

        val setting = parts.settings.filter { it != previousPlace }
            .ifEmpty { parts.settings }
            .let { it[random.nextInt(it.size)] }
        val time = parts.times[random.nextInt(parts.times.size)]
        val weather = parts.weathers[random.nextInt(parts.weathers.size)]
        val place = listOf(setting, time.suffix, weather.suffix).filter { it.isNotBlank() }.joinToString(" ")
        val lines = listOf(time.line, weather.line).filter { it.isNotBlank() } + obstacle.lines

        return Puzzle(
            id = "forge-${obstacle.id}-${java.lang.Long.toHexString(random.nextLong())}",
            place = place,
            lines = lines,
            prompt = obstacle.prompt,
            solutions = solutions,
            hints = hints,
            fallback = obstacle.need,
        )
    }
}
