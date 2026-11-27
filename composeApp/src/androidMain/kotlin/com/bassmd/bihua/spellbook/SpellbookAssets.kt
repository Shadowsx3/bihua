package com.bassmd.bihua.spellbook

import android.content.Context

/** Android reads the bundled CSV; iOS will read the same table through its file seam. */
fun Spellbook.load(context: Context): List<Spell> = load {
    context.assets.open("spellbook.csv").bufferedReader().use { reader ->
        reader.lineSequence().toList().asSequence()
    }
}
