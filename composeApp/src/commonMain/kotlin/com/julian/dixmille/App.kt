package com.julian.dixmille

import androidx.compose.runtime.Composable
import com.julian.dixmille.presentation.theme.DixMilleTheme

/**
 * Main application composable.
 * 
 * Uses Koin for dependency injection and Navigation 3 for type-safe navigation.
 * All navigation logic is handled in Navigator.kt.
 */
@Composable
fun App() {
    DixMilleTheme {
        Navigator()
    }
}
