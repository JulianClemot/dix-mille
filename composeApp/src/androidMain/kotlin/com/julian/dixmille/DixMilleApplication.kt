package com.julian.dixmille

import android.app.Application
import com.julian.dixmille.di.commonModules
import com.julian.dixmille.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DixMilleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin with Android context and modules
        startKoin {
            androidContext(this@DixMilleApplication)
            modules(commonModules() + platformModule)
        }
    }
}
