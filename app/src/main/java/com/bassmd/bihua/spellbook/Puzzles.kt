package com.bassmd.bihua.spellbook

/** One way the world can be outsmarted. A puzzle holds several. */
data class Solution(
    /** Any spell from one of these families works. */
    val families: Set<String>,
    /** What the learner sees happen. */
    val text: String,
)

data class Puzzle(
    val id: String,
    val place: String,
    val lines: List<String>,
    val prompt: String,
    val solutions: List<Solution>,
    /** Shown when a cast cannot help, keyed by family; [fallback] otherwise. */
    val hints: Map<String, String>,
    val fallback: String,
)

/**
 * The whole puzzle library, loaded from `puzzles.csv` and `puzzle_hints.csv`.
 * One row per valid solution keeps the format easy to extend: to add a puzzle,
 * add rows; to add a clever new way to solve it, add one more row.
 */
class PuzzleBook(
    val puzzles: List<Puzzle>,
    /** Structural problems found while loading. Empty for shipped data. */
    val problems: List<String>,
) {
    private val byId = puzzles.associateBy { it.id }

    fun puzzle(id: String): Puzzle? = byId[id]

    /** Puzzles that at least one spell of [family] can solve. */
    fun forFamily(family: String): List<Puzzle> {
        val borrowed = FallbackFamilies[family] ?: family
        val direct = puzzles.filter { puzzle -> puzzle.solutions.any { borrowed in it.families } }
        return direct.ifEmpty { puzzles }
    }

    companion object {

    /**
     * Reads the compiled `puzzles.db` asset. Queries are indexed by family, so a
     * library of thousands costs the same as a few dozen. The CSV remains the
     * authoring format; `tools/spells/build_puzzle_db.py` compiles it.
     */

    fun load(context: android.content.Context): PuzzleBook {
        val problems = mutableListOf<String>()
        val file = java.io.File(context.filesDir, "puzzles.db")
        runCatching {
            context.assets.open("puzzles.db").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure { problems += "could not copy puzzles.db: ${it.message}" }
        if (!file.exists()) return PuzzleBook(emptyList(), problems)

        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            file.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
        )
        val order = mutableListOf<String>()
        val places = HashMap<String, String>()
        val lines = HashMap<String, List<String>>()
        val prompts = HashMap<String, String>()
        val fallbacks = HashMap<String, String>()
        val solutions = HashMap<String, LinkedHashMap<String, MutableSet<String>>>()
        val hints = HashMap<String, MutableMap<String, String>>()

        db.rawQuery("SELECT id, place, lines, prompt, fallback FROM puzzle", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                order += id
                places[id] = c.getString(1)
                lines[id] = c.getString(2).split("|").map { it.trim() }.filter { it.isNotEmpty() }
                prompts[id] = c.getString(3)
                fallbacks[id] = c.getString(4)
            }
        }
        db.rawQuery("SELECT puzzle_id, family, text FROM solution", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val family = c.getString(1)
                val text = c.getString(2)
                if (SpellFamilies.family(family) == null) {
                    problems += "unknown family $family in $id"
                }
                solutions.getOrPut(id) { LinkedHashMap() }
                    .getOrPut(text) { mutableSetOf() }
                    .add(family)
            }
        }
        db.rawQuery("SELECT puzzle_id, family, text FROM hint", null).use { c ->
            while (c.moveToNext()) {
                hints.getOrPut(c.getString(0)) { mutableMapOf() }[c.getString(1)] = c.getString(2)
            }
        }
        db.close()

        val puzzles = order.mapNotNull { id ->
            val solved = solutions[id].orEmpty()
            if (solved.isEmpty()) {
                problems += "puzzle $id has no solutions"
                return@mapNotNull null
            }
            if (solved.size < 2) problems += "puzzle $id has only one way through"
            Puzzle(
                id = id,
                place = places[id].orEmpty().ifEmpty { "Somewhere" },
                lines = lines[id].orEmpty(),
                prompt = prompts[id].orEmpty(),
                solutions = solved.map { (text, families) -> Solution(families.toSet(), text) },
                hints = hints[id].orEmpty(),
                fallback = fallbacks[id].orEmpty(),
            )
        }
        return PuzzleBook(puzzles, problems)
    }


        fun parse(puzzleCsv: String, hintCsv: String = ""): PuzzleBook {
            val problems = mutableListOf<String>()
            val order = mutableListOf<String>()
            val places = HashMap<String, String>()
            val lines = HashMap<String, List<String>>()
            val prompts = HashMap<String, String>()
            // id -> text -> families
            val solutions = HashMap<String, LinkedHashMap<String, MutableSet<String>>>()

            val rows = Csv.parse(puzzleCsv)
            rows.firstOrNull()?.let { header ->
                val expected = listOf("id", "place", "lines", "prompt", "family", "text")
                if (header.size < expected.size || header.take(expected.size) != expected) {
                    problems += "puzzles.csv header must be ${expected.joinToString(",")}"
                }
            }
            rows.drop(1).forEachIndexed { index, cells ->
                if (cells.size < 6) {
                    problems += "puzzles.csv row ${index + 2}: expected 6 columns"
                    return@forEachIndexed
                }
                val id = cells[0].trim()
                if (id.isEmpty()) {
                    problems += "puzzles.csv row ${index + 2}: empty id"
                    return@forEachIndexed
                }
                if (id !in order) order += id
                places.putIfAbsent(id, cells[1].trim())
                lines.putIfAbsent(id, cells[2].split("|").map { it.trim() }.filter { it.isNotEmpty() })
                prompts.putIfAbsent(id, cells[3].trim())
                val family = cells[4].trim()
                val text = cells[5].trim()
                if (SpellFamilies.family(family) == null) {
                    problems += "puzzles.csv row ${index + 2}: unknown family $family"
                }
                if (text.isEmpty()) problems += "puzzles.csv row ${index + 2}: empty solution text"
                solutions.getOrPut(id) { LinkedHashMap() }
                    .getOrPut(text) { mutableSetOf() }
                    .add(family)
            }

            val hints = HashMap<String, MutableMap<String, String>>()
            val fallbacks = HashMap<String, String>()
            if (hintCsv.isNotBlank()) {
                Csv.parse(hintCsv).drop(1).forEachIndexed { index, cells ->
                    if (cells.size < 3) {
                        problems += "puzzle_hints.csv row ${index + 2}: expected 3 columns"
                        return@forEachIndexed
                    }
                    val id = cells[0].trim()
                    val family = cells[1].trim()
                    val text = cells[2].trim()
                    if (family == "*") {
                        fallbacks[id] = text
                    } else {
                        if (SpellFamilies.family(family) == null) {
                            problems += "puzzle_hints.csv row ${index + 2}: unknown family $family"
                        }
                        hints.getOrPut(id) { mutableMapOf() }[family] = text
                    }
                }
            }

            val puzzles = order.mapNotNull { id ->
                val solved = solutions[id].orEmpty()
                if (solved.isEmpty()) {
                    problems += "puzzle $id has no solutions"
                    return@mapNotNull null
                }
                val prompt = prompts[id].orEmpty()
                if (prompt.isEmpty()) problems += "puzzle $id has no prompt"
                if (solved.size < 2) problems += "puzzle $id has only one way through"
                Puzzle(
                    id = id,
                    place = places[id].orEmpty().ifEmpty { "Somewhere" },
                    lines = lines[id].orEmpty(),
                    prompt = prompt,
                    solutions = solved.map { (text, families) -> Solution(families.toSet(), text) },
                    hints = hints[id].orEmpty(),
                    fallback = fallbacks[id].orEmpty()
                        .ifEmpty { "The ink settles and nothing changes. Try a different element." },
                )
            }
            return PuzzleBook(puzzles, problems)
        }
    }
}

/** Families that use another family's puzzles when they have none of their own. */
val FallbackFamilies: Map<String, String> = mapOf(
    "person" to "hand",
    "creature" to "animal",
    "boat" to "water",
    "mountain" to "stone",
    "metal" to "stone",
    "cloud" to "wind",
    "lightning" to "fire",
    "snow" to "ice",
    "rain" to "water",
    "light" to "sun",
    "shadow" to "moon",
    "mind" to "hand",
    "sound" to "wind",
    "dream" to "moon",
    "time" to "stone",
)
