package com.julian.dixmille

import androidx.compose.ui.window.ComposeUIViewController
import com.julian.dixmille.di.commonModules
import com.julian.dixmille.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Initialize Koin before composition
        startKoin {
            modules(commonModules() + platformModule)
        }
    }
) { 
    App()
}