package com.bassmd.bihua.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.settings.UserProfile

/**
 * The learner's pet. Every look is drawn by InkMascot; there is no photo
 * library, file read, or image decoder in the avatar path.
 */
@Composable
fun ProfileAvatar(profile: UserProfile, size: Dp = 56.dp) {
    val style = petStyle(profile.petId)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFFFFDF4)),
        contentAlignment = Alignment.Center,
    ) {
        InkMascot(
            size = size * 0.94f,
            mood = style.mood,
            sparkle = style.sparkle,
            withBrush = style.withBrush,
            headwear = style.headwear,
            halo = style.halo,
        )
    }
}
