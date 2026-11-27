package com.bassmd.bihua.spellbook

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Android copies the shipped puzzle parts out of assets and reads them with the
 * platform SQLite. The authored parts are immutable for the process, so a
 * repeat visit to the road does not reopen and reparse the database.
 */
fun PuzzleParts.Companion.load(context: Context): PuzzleParts? {
    cachedParts()?.let { return it }
    val file = File(context.filesDir, "puzzle_parts.db")
    runCatching {
        context.assets.open("puzzle_parts.db").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    if (!file.exists()) return null
    val db = SQLiteDatabase.openDatabase(
        file.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY,
    )
    val loaded = try {
        runCatching { puzzlePartsFromDatabase(db) }.getOrNull()
    } finally {
        db.close()
    }
    if (loaded == null) {
        file.delete()
        return null
    }
    return rememberParts(loaded)
}

fun PuzzleParts.Companion.fromDatabase(db: SQLiteDatabase): PuzzleParts =
    puzzlePartsFromDatabase(db)

fun puzzlePartsFromDatabase(db: SQLiteDatabase): PuzzleParts {
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
    val settingTags = HashMap<String, MutableSet<String>>()
    db.rawQuery("SELECT place, tag FROM setting_tag", null).use { c ->
        while (c.moveToNext()) {
            settingTags.getOrPut(c.getString(0)) { mutableSetOf() }.add(c.getString(1))
        }
    }
    val obstaclePlaces = HashMap<String, MutableSet<String>>()
    db.rawQuery("SELECT obstacle_id, tag FROM obstacle_place", null).use { c ->
        while (c.moveToNext()) {
            obstaclePlaces.getOrPut(c.getString(0)) { mutableSetOf() }.add(c.getString(1))
        }
    }
    val obstacleSuccess = HashMap<String, String>()
    db.rawQuery("SELECT obstacle_id, text FROM obstacle_success", null).use { c ->
        while (c.moveToNext()) obstacleSuccess[c.getString(0)] = c.getString(1)
    }
    return PuzzleParts(
        names, tags, obstacles, outcomes, settings, times, weathers,
        settingTags, obstaclePlaces, obstacleSuccess,
    )
}
