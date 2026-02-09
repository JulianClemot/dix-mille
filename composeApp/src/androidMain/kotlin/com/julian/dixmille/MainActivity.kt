package com.julian.dixmille

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.julian.dixmille.di.commonModules
import com.julian.dixmille.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Koin with Android context and modules
        startKoin {
            androidContext(applicationContext)
            modules(commonModules() + platformModule)
        }

        setContent {
            App()
        }
    }
}