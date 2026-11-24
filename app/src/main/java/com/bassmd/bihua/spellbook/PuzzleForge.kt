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
    val outcomes: Map<String, List<String>>,
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
            val outcomes = HashMap<String, MutableList<String>>()
            db.rawQuery("SELECT tag, text FROM tag_outcome", null).use { c ->
                while (c.moveToNext()) {
                    outcomes.getOrPut(c.getString(0)) { mutableListOf() }.add(c.getString(1))
                }
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
        character: String = "",
        style: PuzzleStyle = PuzzleStyle.FAMILY,
        previousPlace: String? = null,
    ): Puzzle {
        require(parts.obstacles.isNotEmpty()) { "no obstacles to build a puzzle from" }
        val carried = family?.let { parts.familyTags[it] }.orEmpty()
        val fittingObstacles = parts.obstacles.filter { obstacle -> obstacle.accepts.any { it in carried } }
        val obstacle = (fittingObstacles.ifEmpty { parts.obstacles }).let { it[random.nextInt(it.size)] }

        val fittingFamilies = parts.familyNames.keys
            .mapNotNull { id ->
                val matched = parts.familyTags[id].orEmpty().intersect(obstacle.accepts).sorted()
                if (matched.isEmpty()) return@mapNotNull null
                Triple(id, matched, parts.familyNames[id].orEmpty())
            }
            .shuffled(random)

        val solutionList = when {
            style == PuzzleStyle.CHARACTER && character.isNotEmpty() && family != null -> {
                val match = fittingFamilies.firstOrNull { it.first == family }
                val name = parts.familyNames[family].orEmpty().lowercase()
                val outcome = match?.second?.firstOrNull()
                    ?.let { parts.outcomes[it]?.random(random) }
                    ?: "The {family} works, and the way opens."
                listOf(
                    Solution(
                        families = setOf(family),
                        characters = setOf(character),
                        text = outcome.replace("{family}", name),
                    ),
                )
            }
            style == PuzzleStyle.CATEGORY -> {
                // The scene's own chapter always answers, so its spell can solve
                // the puzzle it introduced. A second chapter widens the door.
                val own = family?.let { SpellFamilies.chapter(it) }
                val others = fittingFamilies
                    .map { SpellFamilies.chapter(it.first) }
                    .filter { it != own }
                    .distinct()
                    .shuffled(random)
                val chapters = (listOfNotNull(own) + others)
                    .distinct()
                    .take(if (random.nextBoolean()) 1 else 2)
                chapters.map { chapter ->
                    Solution(
                        families = emptySet(),
                        categories = setOf(chapter),
                        text = categoryOutcome(chapter),
                    )
                }
            }
            else -> fittingFamilies.map { (id, matched, rawName) ->
                val name = rawName.lowercase()
                val outcome = parts.outcomes[matched.first()]?.random(random)
                    ?: "The {family} works, and the way opens."
                Solution(families = setOf(id), text = outcome.replace("{family}", name))
            }
        }

        val solvedBy = solutionList.flatMap { it.families }.toSet()
        val solvedChapters = solutionList.flatMap { it.categories }.toSet()
        val styleNeed = when (style) {
            PuzzleStyle.CHARACTER -> "Only one exact shape will do here. ${obstacle.need}"
            PuzzleStyle.CATEGORY -> "This one wants a whole chapter of magic, not one narrow family. ${obstacle.need}"
            else -> obstacle.need
        }
        val hints = parts.familyNames.keys
            .filter { it !in solvedBy && SpellFamilies.chapter(it) !in solvedChapters }
            .associateWith { styleNeed }

        val setting = parts.settings
            .filter { previousPlace == null || !previousPlace.startsWith(it) }
            .ifEmpty { parts.settings }
            .let { it[random.nextInt(it.size)] }
        val time = parts.times[random.nextInt(parts.times.size)]
        val weather = parts.weathers[random.nextInt(parts.weathers.size)]
        val place = listOf(setting, time.suffix, weather.suffix).filter { it.isNotBlank() }.joinToString(" ")
        val styleLine = when {
            style == PuzzleStyle.CHARACTER -> "It wants one exact shape, not just the right kind of magic."
            style == PuzzleStyle.CATEGORY -> "It answers to a whole chapter, not one narrow family."
            else -> ""
        }
        val lines = listOf(time.line, weather.line).filter { it.isNotBlank() } +
            listOfNotNull(styleLine.takeIf { it.isNotEmpty() }) + obstacle.lines

        return Puzzle(
            id = "forge-${obstacle.id}-${java.lang.Long.toHexString(random.nextLong())}",
            place = place,
            lines = lines,
            prompt = obstacle.prompt,
            solutions = solutionList,
            hints = hints,
            fallback = styleNeed,
        )
    }

/** The result line when a whole chapter answers, not one family. */
    private fun categoryOutcome(chapter: String): String = when (chapter) {
        "Tides" -> "The tide turns for you, and the way opens through the water."
        "Flame" -> "A warm light answers, and the way clears."
        "Sky" -> "The air moves with you, and the way opens overhead."
        "Ground" -> "The ground settles into a path, and the way holds."
        "Creatures" -> "Something living answers, and the way opens."
        "Craft" -> "A made thing answers, and the way comes together."
        "Mind" -> "A clear thought answers, and the way becomes simple."
        "Time" -> "The moment turns, and the way opens."
        "Arcana" -> "The sign answers, and the way unfolds."
        else -> "A spirit answers, and the way is guarded no longer."
    }
}
