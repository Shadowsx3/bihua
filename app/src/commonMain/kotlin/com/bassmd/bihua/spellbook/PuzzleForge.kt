package com.bassmd.bihua.spellbook

import kotlin.concurrent.Volatile

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
    /** What kind of place each setting is: INDOOR, WATER, FOREST, TOWN, and so on. */
    val settingTags: Map<String, Set<String>> = emptyMap(),
    /** The place kinds an obstacle can honestly happen in. */
    val obstaclePlaces: Map<String, Set<String>> = emptyMap(),
    /** What the scene looks like once this obstacle is answered. */
    val obstacleSuccess: Map<String, String> = emptyMap(),
) {
    companion object {
        @Volatile
        private var cached: PuzzleParts? = null

        fun cachedParts(): PuzzleParts? = cached

        fun rememberParts(parts: PuzzleParts): PuzzleParts {
            cached = parts
            return parts
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
        excludedObstacles: Set<String> = emptySet(),
    ): Puzzle {
        require(parts.obstacles.isNotEmpty()) { "no obstacles to build a puzzle from" }
        val carried = family?.let { parts.familyTags[it] }.orEmpty()
        val fittingObstacles = parts.obstacles.filter { obstacle -> obstacle.accepts.any { it in carried } }
        val candidateObstacles = fittingObstacles.ifEmpty { parts.obstacles }
        val baseObstacles = candidateObstacles
            .filter { it.prompt !in excludedObstacles }
            .ifEmpty { candidateObstacles }
        // A family door must have more than one honest family behind it. If a
        // narrow fixture (or a future part) has only one, prefer a wider
        // obstacle rather than shipping a puzzle with a single answer.
        val familyObstacles = if (style == PuzzleStyle.FAMILY) {
            baseObstacles.filter { obstacle ->
                parts.familyNames.keys.count { id ->
                    parts.familyTags[id].orEmpty().any { it in obstacle.accepts }
                } >= 2
            }.ifEmpty { baseObstacles }
        } else {
            baseObstacles
        }
        val obstacle = familyObstacles.let { it[random.nextInt(it.size)] }

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
                        text = parts.obstacleSuccess[obstacle.id]
                            ?: outcome.replace("{family}", name),
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
                        text = parts.obstacleSuccess[obstacle.id] ?: categoryOutcome(chapter),
                    )
                }
            }
            else -> fittingFamilies.map { (id, matched, rawName) ->
                val name = rawName.lowercase()
                val outcome = parts.outcomes[matched.first()]?.random(random)
                    ?: "The {family} works, and the way opens."
                Solution(
                    families = setOf(id),
                    text = parts.obstacleSuccess[obstacle.id]
                        ?: outcome.replace("{family}", name),
                )
            }
        }

        val solvedBy = solutionList.flatMap { it.families }.toSet()
        val solvedChapters = solutionList.flatMap { it.categories }.toSet()
        val styleNeed = obstacle.need
        val hints = parts.familyNames.keys
            .filter { it !in solvedBy && SpellFamilies.chapter(it) !in solvedChapters }
            .associateWith { styleNeed }

        // A frozen lake does not belong in a copper market. Keep the place
        // pool to settings that share a kind with this obstacle, while still
        // leaving every obstacle many places to happen in.
        val obstacleKinds = parts.obstaclePlaces[obstacle.id].orEmpty()
        val fittingSettings = if (obstacleKinds.isEmpty()) {
            parts.settings
        } else {
            parts.settings.filter { place ->
                parts.settingTags[place].orEmpty().any { it in obstacleKinds }
            }.ifEmpty { parts.settings }
        }
        val setting = fittingSettings
            .filter { previousPlace == null || !previousPlace.startsWith(it) }
            .ifEmpty { fittingSettings }
            .let { it[random.nextInt(it.size)] }
        var time = parts.times[random.nextInt(parts.times.size)]
        var weather = parts.weathers[random.nextInt(parts.weathers.size)]
        repeat(12) {
            if (compatible(setting, time, weather)) return@repeat
            weather = parts.weathers[random.nextInt(parts.weathers.size)]
        }
        if (!compatible(setting, time, weather)) {
            time = parts.times.firstOrNull { it.suffix.isBlank() } ?: time
            weather = parts.weathers.firstOrNull { it.suffix.isBlank() } ?: weather
        }
        // Avoid two "under ..." clauses or a repeated suffix in one place.
        if (time.suffix.startsWith("under") && weather.suffix.startsWith("under")) {
            weather = parts.weathers.firstOrNull { !it.suffix.startsWith("under") } ?: weather
        }
        val partsOfPlace = listOf(setting, time.suffix, weather.suffix)
            .filter { it.isNotBlank() }
            .distinct()
        val place = partsOfPlace.joinToString(" ")
        val styleLine = when (style) {
            PuzzleStyle.CHARACTER ->
                "It wants one exact shape. Only that one character will fit."
            PuzzleStyle.CATEGORY -> {
                val chapters = solutionList.flatMap { it.categories }.distinct()
                if (chapters.isEmpty()) {
                    "It answers to a whole chapter, not one family."
                } else {
                    "It answers to the ${chapters.joinToString(" or ")} chapter. Any character from that chapter will fit."
                }
            }
            PuzzleStyle.FAMILY -> ""
        }
        val lines = listOf(time.line, weather.line).filter { it.isNotBlank() } +
            listOfNotNull(styleLine.takeIf { it.isNotEmpty() }) + obstacle.lines

        return Puzzle(
            id = "forge-${obstacle.id}-${random.nextLong().toULong().toString(16)}",
            place = place,
            lines = lines,
            prompt = obstacle.prompt,
            solutions = solutionList,
            hints = hints,
            fallback = styleNeed,
        )
    }

    /** Keeps the mechanical time/weather combinations from contradicting each other. */
    private fun compatible(setting: String, time: TimeOfDay, weather: Weather): Boolean {
        val s = setting.lowercase()
        val t = (time.suffix + " " + time.line).lowercase()
        val w = (weather.suffix + " " + weather.line).lowercase()
        if ((s.contains("frozen") || s.contains("ice cave")) &&
            ("heat" in w || "warm" in w || "summer" in t || "heat" in t)
        ) return false
        if (s.contains("fog") && "clear" in w) return false
        if ((s.contains("dry field") || s.contains("salt flat")) &&
            ("rain" in w || "drizzle" in w || "snow" in w)
        ) return false
        val clashes = listOf(
            "moon" to "moonless",
            "snow" to "heat", "snow" to "warm", "snow" to "summer",
            "snow" to "drizzle", "snow" to "rain",
            "frost" to "heat", "frost" to "warm", "ice" to "heat",
            "frozen" to "heat", "winter" to "heat", "winter" to "summer",
            "summer" to "frost", "rain" to "dry", "rain" to "drought",
            "storm" to "still", "gale" to "still", "fog" to "clear",
            "mist" to "clear", "rain" to "clear",
        )
        return clashes.none { (a, b) -> (a in t && b in w) || (b in t && a in w) }
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
