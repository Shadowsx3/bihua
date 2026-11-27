package com.bassmd.bihua

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bassmd.bihua.ai.NetworkStatus
import com.bassmd.bihua.ai.QaFlags
import com.bassmd.bihua.diagnostics.CrashContext
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.ui.BihuaApp
import com.bassmd.bihua.ui.LocalEdgeToEdge
import com.bassmd.bihua.ui.components.OpeningInk
import com.bassmd.bihua.ui.LocalFoldState
import com.bassmd.bihua.ui.rememberFoldState
import com.bassmd.bihua.ui.theme.BihuaTheme
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        WidgetUpdater.updateAll(this)
    }

    override fun onStop() {
        super.onStop()
        // Leave the launcher showing today's real counts after a study session.
        WidgetUpdater.updateAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkStatus.init(applicationContext)
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("qa_crash", false) == true) {
            throw IllegalStateException("QA crash on request")
        }
        if (BuildConfig.DEBUG) {
            QaFlags.noInkModel = intent?.getBooleanExtra("qa_no_ink_model", false) == true
            QaFlags.downloadNoticeMs = intent?.getLongExtra("qa_download_notice_ms", 0L) ?: 0L
            QaFlags.slowInkMs = intent?.getLongExtra("qa_slow_ink_ms", 0L) ?: 0L
            QaFlags.contentDelayMs = intent?.getLongExtra("qa_content_delay_ms", 0L) ?: 0L
            QaFlags.missingMode = intent?.getStringExtra("qa_missing_mode")
                ?.let { runCatching { com.bassmd.bihua.content.StudyMode.valueOf(it) }.getOrNull() }
            QaFlags.longProblem = intent?.getBooleanExtra("qa_long_problem", false) == true
            QaFlags.wordProbe = intent?.getStringExtra("qa_word")
            QaFlags.reportProbe = intent?.getBooleanExtra("qa_report", false) == true
            QaFlags.syncHost = intent?.getStringExtra("qa_sync_host")
            QaFlags.syncPort = intent?.getIntExtra("qa_sync_port", 0) ?: 0
        }
        setContent {
            val store = remember { SettingsStore(applicationContext) }
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            LaunchedEffect(settings.studyMode, settings.dayStartHour) {
                CrashContext.update(settings.studyMode.name, settings.dayStartHour)
            }
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val view = LocalView.current
            SideEffect {
                // Before Android 15 the window is inset normally and the theme's
                // dark bars keep their light icons. From Android 15 the bars are
                // transparent, so match the icons to the app's paper or night ink.
                if (Build.VERSION.SDK_INT >= 35) {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
            }
            val foldState = rememberFoldState()
            BihuaTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(
                    LocalEdgeToEdge provides (Build.VERSION.SDK_INT >= 35),
                    LocalFoldState provides foldState,
                ) {
                    Surface(color = Paper) {
                        var opening by rememberSaveable { mutableStateOf(true) }
                        var contentReady by remember { mutableStateOf(false) }
                        // The whole shell follows the IME. Individual screens can
                        // still add their own imePadding for dialog windows.
                        Box(Modifier.fillMaxSize().imePadding()) {
                            BihuaApp(
                                store = store,
                                onContentReady = { contentReady = true },
                                showInkNotice = !opening,
                            )
                            AnimatedVisibility(
                                visible = opening,
                                exit = fadeOut(tween(280)) + scaleOut(targetScale = 1.015f, animationSpec = tween(280)),
                            ) {
                                OpeningInk(
                                    reduceMotion = settings.reduceMotion,
                                    ready = contentReady,
                                    onDone = { opening = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
