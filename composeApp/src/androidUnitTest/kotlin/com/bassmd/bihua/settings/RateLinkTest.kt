package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RateLinkTest {
    @Test
    fun oneHttpsPlayListingUrl() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.bassmd.bihua",
            RateLinks.playListing("com.bassmd.bihua").toString(),
        )
    }
}
