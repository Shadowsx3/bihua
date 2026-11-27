package com.bassmd.bihua.widget

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * The App Group is the only shared container between the app and the WidgetKit
 * extension. The app writes one small JSON document; the extension reads it,
 * so the widget never needs a second database or a network call.
 *
 * `group.com.bassmd.bihua` must stay identical to the App Group in both
 * entitlement files and to `SnapshotStore.suite` in Swift.
 */
object IosWidgetStore {
    const val SUITE = "group.com.bassmd.bihua"
    const val KEY = "widget_snapshot"

    private val json = Json { encodeDefaults = true }

    fun publish(snapshot: SharedWidgetSnapshot): Boolean {
        val defaults = NSUserDefaults(suiteName = SUITE) ?: return false
        defaults.setObject(json.encodeToString(SharedWidgetSnapshot.serializer(), snapshot), forKey = KEY)
        return defaults.synchronize()
    }

    /** P3 host data until the shared data layer publishes real snapshots. */
    @OptIn(ExperimentalTime::class)
    fun publishShowcase(): Boolean = publish(
        showcaseWidgetSnapshot(nowEpochMs = Clock.System.now().toEpochMilliseconds()),
    )
}
