package com.bassmd.bihua

import android.app.Application
import com.bassmd.bihua.diagnostics.CrashHandler
import com.bassmd.bihua.platform.AndroidPlatformServices
import com.bassmd.bihua.platform.Services

class BihuaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Services.install(AndroidPlatformServices(this))
        CrashHandler.install(this)
    }
}
