package com.bassmd.bihua.platform

/** Which client is running. UI asks capabilities, not the OS, for what exists. */
enum class Platform { ANDROID, IOS }

/** Build and device facts, without an Android `Context` or a `UIDevice` in common code. */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val platform: Platform,
    val deviceModel: String,
    val osVersion: String,
    val isDebug: Boolean,
)

/**
 * What this build can do. Every platform-only row and route checks this once,
 * so iOS never shows a Sync or widget entry that cannot work there.
 */
data class Capabilities(
    val localSync: Boolean,
    val widgets: Boolean,
    val foldables: Boolean,
    val crashAttach: Boolean,
    val rateUrl: String?,
)

/** App-private paths and small file reads. Paths are relative to the app sandbox. */
interface AppFiles {
    val filesDir: String
    val cacheDir: String

    fun readBytes(path: String): ByteArray?
    fun writeBytes(path: String, bytes: ByteArray): Boolean
    fun exists(path: String): Boolean
    fun delete(path: String): Boolean
}

fun interface LinkOpener {
    fun open(url: String): Boolean
}

/**
 * The platform seams the shared code is allowed to see. The full catalogue for
 * later phases (recognition, speech, sounds, share, picker, PDF, reminders,
 * network, images, clipboard) lives in `docs/ios-architecture.md`; pieces are
 * added here only when a shared screen needs them.
 */
interface PlatformServices {
    val info: AppInfo
    val files: AppFiles
    val capabilities: Capabilities
    val links: LinkOpener
}

/**
 * Installed once by each app shell before the first Compose frame. Common code
 * never reaches for a platform singleton.
 */
object Services {
    private var installed: PlatformServices? = null

    fun install(services: PlatformServices) {
        installed = services
    }

    fun reset() {
        installed = null
    }

    val installedServices: PlatformServices? get() = installed

    val current: PlatformServices
        get() = installed ?: error("PlatformServices must be installed before the first frame")
}
