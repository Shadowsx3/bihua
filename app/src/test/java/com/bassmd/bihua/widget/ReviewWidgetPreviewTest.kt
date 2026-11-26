package com.bassmd.bihua.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.R
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.DayLoad
import com.bassmd.bihua.progress.HourLoad
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Draws the restored 1.1.1 widgets to PNGs for visual review without a launcher. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewWidgetPreviewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val snapshot = WidgetSnapshot(
        mode = StudyMode.SIMPLIFIED,
        due = 6,
        newLeft = 2,
        word = null,
        vacation = false,
        reviewsDone = 12,
        dueLater = 5,
        lessonsDone = 3,
        nextDueInDays = 1,
        nextDueCount = 8,
        hours = listOf(
            HourLoad(hour = 10, count = 6, offset = 0),
            HourLoad(hour = 15, count = 3, offset = 5),
            HourLoad(hour = 19, count = 2, offset = 9),
        ),
        week = listOf(
            DayLoad(DayOfWeek.WEDNESDAY, 6, 0),
            DayLoad(DayOfWeek.THURSDAY, 12, 1),
            DayLoad(DayOfWeek.FRIDAY, 8, 2),
            DayLoad(DayOfWeek.SATURDAY, 4, 3),
            DayLoad(DayOfWeek.SUNDAY, 2, 4),
            DayLoad(DayOfWeek.MONDAY, 5, 5),
            DayLoad(DayOfWeek.TUESDAY, 3, 6),
        ),
    )

    @Test
    fun drawTheTodayAndWeekWidgets() {
        draw("widget-today.png", weekly = false, widthDp = 155, hour = 10)
        draw("widget-today-night.png", weekly = false, widthDp = 155, hour = 22)
        draw("widget-today-wide.png", weekly = false, widthDp = 250, hour = 10)
        draw("widget-week.png", weekly = true, widthDp = 250, hour = 10)
    }

    private fun draw(name: String, weekly: Boolean, widthDp: Int, hour: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        ReviewWidgetBinder.bind(context, views, snapshot, hour = hour, weekly = weekly)
        val root = views.apply(context, FrameLayout(context))
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (148 * density).toInt()
        root.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val out = File("build/reports/$name")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 1_000)
    }
}
