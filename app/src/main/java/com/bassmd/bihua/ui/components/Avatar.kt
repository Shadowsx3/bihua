@file:Suppress("ProduceStateDoesNotAssignValue") // assignment happens inside withContext

package com.bassmd.bihua.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.settings.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * The learner's avatar. A chosen photo wins; otherwise the ink mascot keeps the
 * header friendly without asking for a photo.
 */
@Composable
fun ProfileAvatar(profile: UserProfile, size: Dp = 56.dp) {
    val photoPath = profile.photoPath
    val density = LocalDensity.current
    val targetPx = with(density) { (size * 3).roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoPath, targetPx) {
        value = withContext(Dispatchers.IO) {
            photoPath
                ?.let { path ->
                    runCatching { decodeAvatar(File(path), targetPx) }.getOrNull()
                }
                ?.asImageBitmap()
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color(0xFFFFFDF4)),
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
            InkMascot(size = size * 0.82f, withBrush = true, sparkle = true)
        }
    }
}

/** Copies a picked image into app storage, downscaled to avatar size, and returns the new path. */
fun saveProfilePhoto(filesDir: File, uri: android.net.Uri, resolver: android.content.ContentResolver): String {
    val directory = File(filesDir, "profile").apply { mkdirs() }
    val target = File(directory, "avatar-${System.currentTimeMillis()}.jpg")
    try {
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read the selected photo.")
        val bitmap = decodeAvatar(target, SAVED_AVATAR_PX, rotate = true)
            ?: error("Could not read the selected photo.")
        try {
            target.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
        } finally {
            bitmap.recycle()
        }
    } catch (failure: Throwable) {
        target.delete()
        throw failure
    }
    directory.listFiles()
        ?.filter { it.name.startsWith("avatar-") && it != target }
        ?.forEach { it.delete() }
    return target.absolutePath
}

private const val SAVED_AVATAR_PX = 768

/**
 * Decodes at most [maxPx] on the long edge, then applies the EXIF rotation so a
 * portrait camera photo does not arrive sideways. A full-resolution camera
 * bitmap can be hundreds of megabytes; an avatar only needs a few hundred.
 */
private fun decodeAvatar(file: File, maxPx: Int, rotate: Boolean = false): Bitmap? {
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
    val oriented = if (rotate) applyExifRotation(file, decoded) else decoded
    val longEdge = max(oriented.width, oriented.height)
    if (longEdge <= maxPx) return oriented
    val scale = maxPx.toFloat() / longEdge
    val scaled = Bitmap.createScaledBitmap(
        oriented,
        (oriented.width * scale).toInt().coerceAtLeast(1),
        (oriented.height * scale).toInt().coerceAtLeast(1),
        true,
    )
    if (scaled !== oriented) oriented.recycle()
    return scaled
}

private fun sampleSize(width: Int, height: Int, maxPx: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxPx) sample *= 2
    return sample
}

private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
    val orientation = runCatching {
        ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    val rotated = runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrNull() ?: return bitmap
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}
