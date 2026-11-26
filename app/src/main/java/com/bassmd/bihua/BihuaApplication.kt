package com.bassmd.bihua

import android.app.Application
import com.bassmd.bihua.diagnostics.CrashHandler

class BihuaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
