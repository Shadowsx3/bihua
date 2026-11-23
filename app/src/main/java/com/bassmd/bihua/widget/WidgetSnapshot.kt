package com.bassmd.bihua.widget

import android.content.Context
import com.bassmd.bihua.content.SqliteContentRepository
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.Vacation
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetSnapshot(
    val mode: StudyMode,
    val due: Int,
    val newLeft: Int,
    val word: WordContext?,
    val vacation: Boolean,
    val placeholder: Boolean = false,
) {
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
                    val due = progress.dueCount(visible, settings)
                    val learned = progress.newLearnedToday(
                        mode = settings.studyMode,
                        dayStartHour = settings.dayStartHour,
                        includeSharedGlyphs = settings.shareIdenticalGlyphs,
                    )
                    val remainingNew = visible.count { it.status == StudyStatus.NEW }
                    val newLeft = (settings.dailyNewGoal - learned)
                        .coerceAtLeast(0)
                        .coerceAtMost(remainingNew)
                    val word = repository.featuredWord(
                        mode = settings.studyMode,
                        includeMature = false,
                        seed = LocalDate.now().toEpochDay().toInt(),
                    )
                    WidgetSnapshot(
                        mode = settings.studyMode,
                        due = due,
                        newLeft = newLeft,
                        word = word,
                        vacation = Vacation.isActive(settings.vacationStartedAtMs),
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
            placeholder = true,
        )
    }
}
