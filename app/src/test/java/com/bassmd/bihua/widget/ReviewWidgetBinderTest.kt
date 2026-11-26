package com.bassmd.bihua.widget

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.R
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.DayLoad
import com.bassmd.bihua.progress.HourLoad
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The 1.1.1 widget, now with daily and weekly amounts and a short forecast. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewWidgetBinderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun snapshot(
        reviewsDone: Int = 12,
        due: Int = 6,
        dueLater: Int = 5,
        lessonsDone: Int = 3,
        newLeft: Int = 2,
        vacation: Boolean = false,
        nextDueInDays: Int? = null,
        nextDueCount: Int = 0,
        hours: List<HourLoad> = listOf(
            HourLoad(hour = 10, count = 6, offset = 0),
            HourLoad(hour = 15, count = 3, offset = 5),
            HourLoad(hour = 19, count = 2, offset = 9),
        ),
        week: List<DayLoad> = listOf(
            DayLoad(DayOfWeek.WEDNESDAY, 6, 0),
            DayLoad(DayOfWeek.THURSDAY, 12, 1),
            DayLoad(DayOfWeek.FRIDAY, 8, 2),
            DayLoad(DayOfWeek.SATURDAY, 4, 3),
            DayLoad(DayOfWeek.SUNDAY, 2, 4),
            DayLoad(DayOfWeek.MONDAY, 5, 5),
            DayLoad(DayOfWeek.TUESDAY, 3, 6),
        ),
    ) = WidgetSnapshot(
        mode = StudyMode.SIMPLIFIED,
        due = due,
        newLeft = newLeft,
        word = null,
        vacation = vacation,
        reviewsDone = reviewsDone,
        dueLater = dueLater,
        lessonsDone = lessonsDone,
        nextDueInDays = nextDueInDays,
        nextDueCount = nextDueCount,
        hours = hours,
        week = week,
    )

    private fun render(snapshot: WidgetSnapshot, weekly: Boolean = false): View {
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_today)
        ReviewWidgetBinder.bind(context, views, snapshot, hour = 10, weekly = weekly)
        return views.apply(context, FrameLayout(context))
    }

    @Test
    fun theDailyWidgetKeepsTheTwoBigNumbers() {
        val root = render(snapshot())
        assertEquals("12", root.text(R.id.widget_reviews_count))
        assertEquals("of 23 reviews", root.text(R.id.widget_reviews_label))
        assertEquals("3", root.text(R.id.widget_lessons_count))
        assertEquals("of 5 lessons", root.text(R.id.widget_lessons_label))
        assertEquals("6 now · 3 at 15h · 2 later", root.text(R.id.widget_detail))
        assertEquals("Good morning", root.text(R.id.widget_greeting))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_reviews_block).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_clear).visibility)
    }

    @Test
    fun theWeekWidgetKeepsTheSameShape() {
        val root = render(snapshot(), weekly = true)
        assertEquals("40", root.text(R.id.widget_reviews_count))
        assertEquals("next 7 days", root.text(R.id.widget_reviews_label))
        assertEquals("3", root.text(R.id.widget_lessons_count))
        assertEquals("of 5 lessons", root.text(R.id.widget_lessons_label))
        assertEquals("Today 6 · Thu 12 · 22 more", root.text(R.id.widget_detail))
    }

    @Test
    fun aFinishedDayStillShowsTheAmount() {
        val root = render(
            snapshot(
                reviewsDone = 23,
                due = 0,
                dueLater = 0,
                lessonsDone = 5,
                newLeft = 0,
                hours = emptyList(),
                nextDueInDays = 1,
                nextDueCount = 4,
            ),
        )
        assertEquals("23", root.text(R.id.widget_reviews_count))
        assertEquals("of 23 reviews", root.text(R.id.widget_reviews_label))
        assertEquals("Next tomorrow · 4", root.text(R.id.widget_detail))
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_clear).visibility)
    }

    @Test
    fun anUntouchedDaySaysAllClear() {
        val root = render(
            snapshot(
                reviewsDone = 0,
                due = 0,
                dueLater = 0,
                lessonsDone = 0,
                newLeft = 0,
                hours = emptyList(),
                week = emptyList(),
            ),
        )
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_stats).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_clear).visibility)
        assertEquals("All clear", root.text(R.id.widget_clear))
    }

    @Test
    fun vacationUsesTheOriginalCalmCopy() {
        val root = render(snapshot(vacation = true))
        assertEquals("Vacation", root.text(R.id.widget_greeting))
        assertEquals("Reviews paused", root.text(R.id.widget_clear))
        assertEquals("The desk will wait.", root.text(R.id.widget_detail))
        assertEquals("Open", root.text(R.id.widget_action))
    }

    @Test
    fun theForecastLineStaysShort() {
        assertEquals("6 due now", ReviewWidgetBinder.forecastLine(snapshot(dueLater = 0, hours = emptyList()), weekly = false))
        assertEquals(
            "Next tomorrow · 4",
            ReviewWidgetBinder.forecastLine(
                snapshot(due = 0, dueLater = 0, hours = emptyList(), nextDueInDays = 1, nextDueCount = 4),
                weekly = false,
            ),
        )
        assertEquals("Done for today.", ReviewWidgetBinder.forecastLine(snapshot(due = 0, dueLater = 0, hours = emptyList()), weekly = false))
        assertEquals("in 3 days", ReviewWidgetBinder.relativeDay(3))
    }

    @Test
    fun theLayoutFitsAShortTwoByTwoSlot() {
        val root = render(snapshot())
        layout(root, widthDp = 155, heightDp = 140)
        val action = root.findViewById<View>(R.id.widget_action)
        assertTrue("action ended at ${action.bottom}", action.bottom <= root.height)
        val label = root.findViewById<View>(R.id.widget_lessons_label)
        assertTrue("label ended at ${label.bottom}", label.bottom <= root.height)
        assertTrue("label is hidden", label.height > 0)
    }

    private fun layout(root: View, widthDp: Int, heightDp: Int) {
        val density = context.resources.displayMetrics.density
        root.measure(
            MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((heightDp * density).toInt(), MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, (widthDp * density).toInt(), (heightDp * density).toInt())
    }

    private fun View.text(id: Int): CharSequence = findViewById<TextView>(id).text
}
