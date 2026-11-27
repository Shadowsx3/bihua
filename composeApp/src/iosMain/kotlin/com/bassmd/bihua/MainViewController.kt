package com.bassmd.bihua

import androidx.compose.ui.window.ComposeUIViewController
import com.bassmd.bihua.platform.IosPlatformServices
import com.bassmd.bihua.platform.Services
import platform.UIKit.UIViewController

/**
 * iOS entry point. Each native tab asks for one route; the native shell keeps
 * the tab bar and navigation stack, while the screen content is shared Compose.
 */
fun MainViewController(
    route: String,
    onSelectTab: (String) -> Unit,
    onPush: (String) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onPop: () -> Unit,
    onShareFile: (String) -> Unit,
    onRequestFilePicker: () -> Unit,
    onOnboardingChanged: (Boolean) -> Unit,
    onRenderProgressPdf: (String) -> Unit,
): UIViewController {
    Services.install(IosPlatformServices())
    return ComposeUIViewController {
        IosDynamicType {
            IosRouteScreen(
                routeId = route,
                onSelectTab = onSelectTab,
                onPush = onPush,
                onThemeChange = onThemeChange,
                onPop = onPop,
                onShareFile = onShareFile,
                onRequestFilePicker = onRequestFilePicker,
                onOnboardingChanged = onOnboardingChanged,
                onRenderProgressPdf = onRenderProgressPdf,
            )
        }
    }
}
