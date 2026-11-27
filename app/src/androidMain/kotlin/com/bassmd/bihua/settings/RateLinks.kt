package com.bassmd.bihua.settings

import android.net.Uri

/** One https listing URL; Play App Links opens the store app when it exists. */
object RateLinks {
    fun playListing(packageId: String): Uri =
        Uri.parse("https://play.google.com/store/apps/details?id=$packageId")
}
