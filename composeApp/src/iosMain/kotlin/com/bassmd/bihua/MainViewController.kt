package com.bassmd.bihua

import androidx.compose.ui.window.ComposeUIViewController
import com.bassmd.bihua.platform.IosPlatformServices
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.widget.IosWidgetStore
import platform.UIKit.UIViewController

/**
 * iOS entry point. Each native tab asks for one route; the native shell keeps
 * the tab bar and navigation stack, while the screen content is shared Compose.
 */
fun MainViewController(
    route: String,
    onSelectTab: (String) -> Unit,
    onPush: (String) -> Unit,
): UIViewController {
    Services.install(IosPlatformServices())
    // The widget reads the shared App Group document; P2 replaces this sample
    // with the real settings/progress snapshot.
    IosWidgetStore.publishShowcase()
    return ComposeUIViewController {
        IosDynamicType { IosRouteScreen(route, onSelectTab, onPush) }
    }
}
