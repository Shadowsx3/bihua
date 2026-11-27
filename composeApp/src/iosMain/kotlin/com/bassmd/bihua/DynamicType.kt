package com.bassmd.bihua

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityMedium
import platform.UIKit.UIContentSizeCategoryDidChangeNotification
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraSmall
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryMedium
import platform.UIKit.UIContentSizeCategorySmall
import platform.UIKit.UIApplication

/**
 * Follows the iOS Settings > Display & Text Size > Text Size (Dynamic Type)
 * slider. Compose text is measured in sp, so the content-size category is
 * translated to a font scale; every screen then grows with the system setting
 * the same way a SwiftUI screen would.
 *
 * The scale is relative to the iOS "Large" default, the Apple body-text
 * metrics. Only sp scales; dp spacing stays fixed so cards keep their shape.
 */
@Composable
fun IosDynamicType(content: @Composable () -> Unit) {
    var fontScale by remember { mutableStateOf(preferredFontScale()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIContentSizeCategoryDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            fontScale = preferredFontScale()
        }
        onDispose { center.removeObserver(observer) }
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = fontScale),
        content = content,
    )
}

internal fun preferredFontScale(): Float =
    fontScaleForCategory(UIApplication.sharedApplication.preferredContentSizeCategory)

/** The Apple body-text multipliers for each content-size category. */
internal fun fontScaleForCategory(category: String?): Float = when (category) {
    UIContentSizeCategoryExtraSmall -> 0.82f
    UIContentSizeCategorySmall -> 0.88f
    UIContentSizeCategoryMedium -> 0.94f
    UIContentSizeCategoryLarge -> 1.0f
    UIContentSizeCategoryExtraLarge -> 1.12f
    UIContentSizeCategoryExtraExtraLarge -> 1.24f
    UIContentSizeCategoryExtraExtraExtraLarge -> 1.35f
    UIContentSizeCategoryAccessibilityMedium -> 1.65f
    UIContentSizeCategoryAccessibilityLarge -> 1.94f
    UIContentSizeCategoryAccessibilityExtraLarge -> 2.35f
    UIContentSizeCategoryAccessibilityExtraExtraLarge -> 2.76f
    UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 3.12f
    else -> 1.0f
}
