package com.bassmd.bihua

import androidx.compose.ui.window.ComposeUIViewController
import com.bassmd.bihua.platform.IosPlatformServices
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.ui.SharedHomeShowcase
import platform.UIKit.UIViewController

/**
 * iOS entry point. The Compose UI is the shared Bihua screen; the native shell
 * (tab bar and navigation stack) wraps each route in P3.
 */
fun MainViewController(): UIViewController {
    Services.install(IosPlatformServices())
    return ComposeUIViewController {
        IosDynamicType { SharedHomeShowcase() }
    }
}
