package com.bassmd.bihua.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.bassmd.bihua.BuildConfig
import java.io.File

/**
 * Android implementation of the shared seams. Behaviour is unchanged from the
 * Activity and Application; this only exposes the same facts through commonMain.
 */
class AndroidPlatformServices(private val context: Context) : PlatformServices {
    override val info = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
        platform = Platform.ANDROID,
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        isDebug = BuildConfig.DEBUG,
    )

    override val files: AppFiles = object : AppFiles {
        override val filesDir: String = context.filesDir.absolutePath
        override val cacheDir: String = context.cacheDir.absolutePath

        override fun readBytes(path: String): ByteArray? =
            runCatching { File(filesDir, path).readBytes() }.getOrNull()

        override fun writeBytes(path: String, bytes: ByteArray): Boolean = runCatching {
            val file = File(filesDir, path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            true
        }.getOrDefault(false)

        override fun exists(path: String): Boolean = File(filesDir, path).exists()

        override fun delete(path: String): Boolean =
            runCatching { File(filesDir, path).delete() }.getOrDefault(false)
    }

    override val capabilities = Capabilities(
        localSync = true,
        widgets = true,
        foldables = true,
        crashAttach = true,
        rateUrl = "https://play.google.com/store/apps/details?id=com.bassmd.bihua",
    )

    override val links = LinkOpener { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }
}
