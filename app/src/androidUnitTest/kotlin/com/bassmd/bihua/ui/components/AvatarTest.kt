package com.bassmd.bihua.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.math.max
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AvatarTest {
    private lateinit var context: Context
    private lateinit var source: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "profile").deleteRecursively()
        source = File(context.cacheDir, "avatar-source.png")
        val bitmap = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(46, 125, 107))
        }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @After
    fun tearDown() {
        source.delete()
        File(context.filesDir, "profile").deleteRecursively()
    }

    @Test
    fun savedAvatarIsDownscaledAndOldFilesAreRemoved() {
        val directory = File(context.filesDir, "profile").apply { mkdirs() }
        val old = File(directory, "avatar-1.jpg").apply { writeText("old") }

        val path = saveProfilePhoto(context.filesDir, Uri.fromFile(source), context.contentResolver)

        assertTrue(File(path).isFile)
        assertFalse(old.exists())
        val decoded = BitmapFactory.decodeFile(path)
        assertTrue(max(decoded.width, decoded.height) <= 768)
        assertTrue(decoded.width > 700)
        assertEquals(1.5f, decoded.width.toFloat() / decoded.height, 0.02f)
    }
}
