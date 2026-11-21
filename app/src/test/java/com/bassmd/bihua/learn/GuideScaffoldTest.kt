package com.bassmd.bihua.learn

import com.bassmd.bihua.settings.GuideMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideScaffoldTest {

    @Test
    fun newCardsGetTheFullGuide() {
        assertEquals(GuideScaffold.FULL, GuideScaffold.forReps(0))
    }

    @Test
    fun earlyReviewsGetTheOutlineOnly() {
        assertEquals(GuideScaffold.OUTLINE, GuideScaffold.forReps(1))
        assertEquals(GuideScaffold.OUTLINE, GuideScaffold.forReps(3))
    }

    @Test
    fun matureCardsGetNoGuide() {
        assertEquals(GuideScaffold.HIDDEN, GuideScaffold.forReps(4))
        assertEquals(GuideScaffold.HIDDEN, GuideScaffold.forReps(20))
    }

    @Test
    fun eachLearnerChoiceKeepsItsLevel() {
        assertEquals(GuideScaffold.FULL, GuideScaffold.forMode(GuideMode.FULL, 9))
        assertEquals(GuideScaffold.OUTLINE, GuideScaffold.forMode(GuideMode.OUTLINE, 0))
        assertEquals(GuideScaffold.HIDDEN, GuideScaffold.forMode(GuideMode.HIDDEN, 0))
    }

    @Test
    fun fadeFollowsCardMaturity() {
        assertEquals(GuideScaffold.FULL, GuideScaffold.forMode(GuideMode.FADE, 0))
        assertEquals(GuideScaffold.OUTLINE, GuideScaffold.forMode(GuideMode.FADE, 2))
        assertEquals(GuideScaffold.HIDDEN, GuideScaffold.forMode(GuideMode.FADE, 6))
    }
}
