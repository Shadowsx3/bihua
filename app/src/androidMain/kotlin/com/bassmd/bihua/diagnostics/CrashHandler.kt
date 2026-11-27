package com.bassmd.bihua.diagnostics

import android.content.Context
import android.os.Build
import com.bassmd.bihua.BuildConfig
import java.util.Locale

/** The settings bits the handler may read. Never touch DataStore on a crash. */
object CrashContext {
    @Volatile
    var course: String? = null

    @Volatile
    var dayStartHour: Int? = null

    fun update(course: String, dayStartHour: Int) {
        this.course = course
        this.dayStartHour = dayStartHour
    }
}

/**
 * Installs a local crash saver and always chains to the previous handler, so
 * Play vitals and the system crash dialog behave exactly as before.
 */
object CrashInfo {
    fun current(thread: String = Thread.currentThread().name): CrashMeta = CrashMeta(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        debug = BuildConfig.DEBUG,
        course = CrashContext.course,
        dayStartHour = CrashContext.dayStartHour,
        device = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidRelease = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        locale = Locale.getDefault().toString(),
        whenIso = CrashReport.currentTimeIso(),
        thread = thread,
    )
}

object CrashHandler {
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = CrashReport.format(
                    meta = CrashInfo.current(thread.name),
                    trace = throwable.stackTraceToString(),
                )
                CrashLog.write(app, report)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
