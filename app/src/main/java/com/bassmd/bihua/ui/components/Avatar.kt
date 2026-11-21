@file:Suppress("ProduceStateDoesNotAssignValue") // assignment happens inside withContext

package com.bassmd.bihua.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The learner's avatar. A chosen photo wins; otherwise the ink mascot keeps the
 * header friendly without asking for a photo.
 */
@Composable
fun ProfileAvatar(profile: UserProfile, size: Dp = 56.dp) {
    val photoPath = profile.photoPath
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = withContext(Dispatchers.IO) {
            photoPath
                ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                ?.asImageBitmap()
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Sky.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            InkMascot(size = size * 0.82f)
        }
    }
}

/** Copies a picked image into app storage and returns the new path. */
fun saveProfilePhoto(filesDir: File, uri: android.net.Uri, resolver: android.content.ContentResolver): String {
    val directory = File(filesDir, "profile").apply { mkdirs() }
    val target = File(directory, "avatar-${System.currentTimeMillis()}.jpg")
    resolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Could not read the selected photo.")
    directory.listFiles()
        ?.filter { it.name.startsWith("avatar-") && it != target }
        ?.forEach { it.delete() }
    return target.absolutePath
}
