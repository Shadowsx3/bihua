@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Bounds are reported by the live app and read by the overlay. Entries are
 * removed when their composable leaves, so a stale anchor from a previous
 * screen or an earlier UI state can never highlight an empty spot. Each anchor
 * also carries a [BringIntoViewRequester] so a step can scroll its target into
 * view before the spotlight is drawn.
 */
object TourAnchors {
    val bounds = mutableStateMapOf<String, Rect>()
    private val requesters = mutableMapOf<String, BringIntoViewRequester>()

    internal fun register(id: String, requester: BringIntoViewRequester) {
        requesters[id] = requester
    }

    internal fun unregister(id: String, requester: BringIntoViewRequester) {
        if (requesters[id] === requester) {
            requesters.remove(id)
            bounds.remove(id)
        }
    }

    internal fun requester(id: String): BringIntoViewRequester? = requesters[id]
}

fun Modifier.tourAnchor(id: String): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(id, requester) {
        TourAnchors.register(id, requester)
        onDispose { TourAnchors.unregister(id, requester) }
    }
    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            TourAnchors.bounds[id] = coordinates.boundsInWindow()
        }
}
