package com.bassmd.bihua.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** One spacing scale for the whole app. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Three radii, nothing else. */
object Radius {
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(26.dp)
    val pill = RoundedCornerShape(50)
}

object Elevation {
    val card = 0.dp
    val floating = 3.dp
}

/** Minimum touch target for any control. */
val TouchTarget = 48.dp

/** Accent roles, so colourful surfaces always carry dark text. */
object Accent {
    val soft = listOf(Mint, Peach, Sky, Lilac, Butter, Rose, Gem)
}
