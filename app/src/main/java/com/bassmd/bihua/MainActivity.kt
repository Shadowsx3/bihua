package com.bassmd.bihua

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.ui.BihuaApp
import com.bassmd.bihua.ui.LocalEdgeToEdge
import com.bassmd.bihua.ui.theme.BihuaTheme
import com.bassmd.bihua.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { SettingsStore(applicationContext) }
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
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
            BihuaTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(
                    LocalEdgeToEdge provides (Build.VERSION.SDK_INT >= 35),
                ) {
                    Surface(color = Paper) {
                        BihuaApp(store = store)
                    }
                }
            }
        }
    }
}
