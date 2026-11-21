package com.bassmd.bihua.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode

/** Whether Chinese readings are shown as `ni3hao3` instead of `nǐhǎo`. */
val LocalPinyinNumbers = staticCompositionLocalOf { true }

/** Reading in the learner's preferred notation. Other scripts are unchanged. */
@Composable
fun displayReading(reading: String, mode: StudyMode): String =
    Pinyin.display(reading, mode, LocalPinyinNumbers.current)
