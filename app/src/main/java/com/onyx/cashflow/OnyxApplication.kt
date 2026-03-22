package com.onyx.cashflow

import android.app.Application
import com.onyx.cashflow.utils.OnyxLogger

class OnyxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the on-device logger first so crash handler is ready
        // before any other app code runs.
        OnyxLogger.init(this)
        OnyxLogger.i("OnyxApplication", "Application started")
    }
}
