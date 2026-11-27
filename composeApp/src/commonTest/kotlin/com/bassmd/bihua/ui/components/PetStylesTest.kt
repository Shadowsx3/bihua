package com.bassmd.bihua.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetStylesTest {
    @Test
    fun twelveDistinctLooks() {
        assertEquals(12, PetStyles.size)
        assertEquals(12, PetStyles.map { it.id }.toSet().size)
        assertEquals(DefaultPetId, PetStyles.first().id)
    }

    @Test
    fun everyLookHasCopyAndAVisualDifference() {
        assertTrue(PetStyles.all { it.name.isNotBlank() && it.blurb.isNotBlank() })
        assertTrue(PetStyles.map { Triple(it.mood, it.headwear, it.halo) }.toSet().size >= 11)
    }

    @Test
    fun unknownIdFallsBackToTheFirstLook() {
        assertEquals(DefaultPetId, petStyle("not-a-pet").id)
        assertEquals(DefaultPetId, petStyle(null).id)
    }
}
