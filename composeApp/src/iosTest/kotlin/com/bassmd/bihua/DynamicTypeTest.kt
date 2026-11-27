package com.bassmd.bihua

import kotlin.test.Test
import kotlin.test.assertEquals
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategorySmall

class DynamicTypeTest {
    @Test
    fun theDefaultCategoryLeavesTextUnchanged() {
        assertEquals(1.0f, fontScaleForCategory(UIContentSizeCategoryLarge))
    }

    @Test
    fun smallerAndLargerCategoriesMoveInTheRightDirection() {
        assertEquals(0.88f, fontScaleForCategory(UIContentSizeCategorySmall))
        assertEquals(1.35f, fontScaleForCategory(UIContentSizeCategoryExtraExtraExtraLarge))
        assertEquals(3.12f, fontScaleForCategory(UIContentSizeCategoryAccessibilityExtraExtraExtraLarge))
    }

    @Test
    fun anUnknownOrMissingCategoryFallsBackToDefault() {
        assertEquals(1.0f, fontScaleForCategory("UICTContentSizeCategoryFuture"))
        assertEquals(1.0f, fontScaleForCategory(null))
    }
}
