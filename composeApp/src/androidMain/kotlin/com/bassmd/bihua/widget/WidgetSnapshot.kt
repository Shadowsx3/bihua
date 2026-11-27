package com.bassmd.bihua.widget

import android.content.Context
import com.bassmd.bihua.content.SqliteContentRepository
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.progress.HourLoad
import com.bassmd.bihua.progress.DayLoad
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.todayJava
import com.bassmd.bihua.settings.MascotOutfit
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.Vacation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetSnapshot(
    val mode: StudyMode,
    val due: Int,
    val newLeft: Int,
    val word: WordContext?,
    val vacation: Boolean,
    val outfit: MascotOutfit = MascotOutfit.AUTO,
    val placeholder: Boolean = false,
    val dayStartHour: Int = 4,
    val reviewsDone: Int = 0,
    val dueLater: Int = 0,
    val lessonsDone: Int = 0,
    val nextDueInDays: Int? = null,
    val nextDueCount: Int = 0,
    val hours: List<HourLoad> = emptyList(),
    val week: List<DayLoad> = emptyList(),
) {
    val reviewsLeft: Int get() = due + dueLater
    val reviewsTotal: Int get() = reviewsDone + reviewsLeft
    val lessonsTotal: Int get() = lessonsDone + newLeft
    val weekTotal: Int get() = week.sumOf { it.count }

    companion object {
        suspend fun load(context: Context): WidgetSnapshot? = withContext(Dispatchers.IO) {
            runCatching {
                val settings = SettingsStore(context).currentSettings()
                val repository = SqliteContentRepository(context)
                val progress = ProgressStore(context)
                try {
                    val characters = repository.characters(settings.studyMode)
                    // Match Home: skipped and suspended characters are hidden,
                    // so the widget never promises reviews the app will not show.
                    val visible = characters.filterNot { settings.isSkipped(it.id) }
                    val load = progress.todayLoad(visible, settings)
                    val learned = progress.newLearnedToday(
                        mode = settings.studyMode,
                        dayStartHour = settings.dayStartHour,
                        includeSharedGlyphs = settings.shareIdenticalGlyphs,
                    )
                    val remainingNew = visible.count { it.status == StudyStatus.NEW }
                    val onVacation = Vacation.isActive(settings.vacationStartedAtMs)
                    // Paused means paused: keep today's finished count, but do
                    // not offer reviews or lessons while the desk is resting.
                    val newLeft = if (onVacation) {
                        0
                    } else {
                        (settings.dailyNewGoal - learned)
                            .coerceAtLeast(0)
                            .coerceAtMost(remainingNew)
                    }
                    val word = repository.featuredWord(
                        mode = settings.studyMode,
                        includeMature = false,
                        epochDay = settings.todayJava().toEpochDay().toInt(),
                    )
                    WidgetSnapshot(
                        mode = settings.studyMode,
                        due = if (onVacation) 0 else load.dueNow,
                        newLeft = newLeft,
                        word = word,
                        vacation = onVacation,
                        outfit = settings.mascotOutfit,
                        dayStartHour = settings.dayStartHour,
                        reviewsDone = load.reviewsDone,
                        dueLater = if (onVacation) 0 else load.dueLater,
                        lessonsDone = learned,
                        nextDueInDays = load.nextDueInDays,
                        nextDueCount = load.nextDueCount,
                        hours = if (onVacation) emptyList() else load.hours,
                        week = if (onVacation) emptyList() else load.week,
                    )
                } finally {
                    runCatching { repository.close() }
                    runCatching { progress.close() }
                }
            }.getOrNull()
        }

        fun placeholder(): WidgetSnapshot = WidgetSnapshot(
            mode = StudyMode.SIMPLIFIED,
            due = 0,
            newLeft = 0,
            word = null,
            vacation = false,
            outfit = MascotOutfit.AUTO,
            placeholder = true,
        )
    }
}
