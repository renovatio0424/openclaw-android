package com.tungtung.openclawcompanion

import android.app.Application
import android.content.Context

class OpenClawCompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        @Volatile
        private var appContext: Context? = null

        fun context(): Context = requireNotNull(appContext) {
            "Application context not initialized"
        }
    }
}
