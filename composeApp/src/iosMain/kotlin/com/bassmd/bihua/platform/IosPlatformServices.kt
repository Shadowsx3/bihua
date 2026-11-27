@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.bassmd.bihua.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.posix.memcpy
import kotlin.native.Platform as NativePlatform

/**
 * iOS implementation of the shared seams. Uses Foundation only, so no Swift is
 * needed for paths, build info, or the rate link. Recognition, speech, share,
 * picker, PDF, and reminders arrive as Swift protocols in P4.
 */
class IosPlatformServices : PlatformServices {
    override val info: AppInfo = run {
        val bundle = NSBundle.mainBundle
        val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0"
        val build = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L
        val device = UIDevice.currentDevice
        AppInfo(
            versionName = version,
            versionCode = build,
            platform = Platform.IOS,
            deviceModel = device.model,
            osVersion = "${device.systemName} ${device.systemVersion}",
            isDebug = NativePlatform.isDebugBinary,
        )
    }

    override val files: AppFiles = IosAppFiles()

    override val capabilities = Capabilities(
        localSync = false,
        widgets = true,
        foldables = false,
        crashAttach = false,
        // Filled in when the App Store record exists.
        rateUrl = null,
    )

    override val links = LinkOpener { url ->
        val target = NSURL.URLWithString(url) ?: return@LinkOpener false
        UIApplication.sharedApplication.openURL(target)
    }
}

private class IosAppFiles : AppFiles {
    private val manager = NSFileManager.defaultManager

    private fun directory(search: ULong): String {
        val path = NSSearchPathForDirectoriesInDomains(search, NSUserDomainMask, true)
            .firstOrNull() as? String
            ?: error("Foundation did not return a sandbox directory")
        manager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        // "Your data does not go to iCloud" must stay true on both platforms.
        NSURL.fileURLWithPath(path).setResourceValue(
            true,
            forKey = NSURLIsExcludedFromBackupKey,
            error = null,
        )
        return path
    }

    override val filesDir: String by lazy { directory(NSApplicationSupportDirectory) }
    override val cacheDir: String by lazy { directory(NSCachesDirectory) }

    override fun readBytes(path: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile("$filesDir/$path") ?: return null
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        val out = ByteArray(size)
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return out
    }

    override fun writeBytes(path: String, bytes: ByteArray): Boolean {
        val data = if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
        }
        val target = "$filesDir/$path"
        manager.createDirectoryAtPath(
            target.substringBeforeLast('/', missingDelimiterValue = filesDir),
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return data.writeToFile(target, atomically = true)
    }

    override fun exists(path: String): Boolean = manager.fileExistsAtPath("$filesDir/$path")

    override fun delete(path: String): Boolean =
        manager.removeItemAtPath("$filesDir/$path", error = null)
}
