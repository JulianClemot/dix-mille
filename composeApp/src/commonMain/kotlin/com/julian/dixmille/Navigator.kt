package com.julian.dixmille

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.julian.dixmille.presentation.screen.GameEndEntryPoint
import com.julian.dixmille.presentation.screen.GameEndRoute
import com.julian.dixmille.presentation.screen.GameSetupEntryPoint
import com.julian.dixmille.presentation.screen.GameSetupRoute
import com.julian.dixmille.presentation.screen.HomeEntryPoint
import com.julian.dixmille.presentation.screen.HomeRoute
import com.julian.dixmille.presentation.screen.ScoreSheetEntryPoint
import com.julian.dixmille.presentation.screen.ScoreSheetRoute
import kotlinx.coroutines.launch

/**
 * Main navigator for the Dix Mille app using Navigation 3.
 *
 * Uses type-safe navigation with @Serializable routes and provides:
 * - Centralized navigation logic
 * - Back stack management
 * - Centralized error handling via Snackbar
 */
@Composable
fun Navigator() {
    val backStack = rememberNavBackStack(HomeRoute)
    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LaunchedEffect(null) {
            //TODO handle a global app state to display a snackbar when I receive an error in the navigation events
        }
        NavDisplay(
            backStack = backStack,
            onBack = {
                // Exit app if on HomeRoute, otherwise pop back stack
                val current = backStack.lastOrNull()
                if (current !is HomeRoute) {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {
                // Home Screen
                entry<HomeRoute> {
                    HomeEntryPoint(backStack = backStack)
                }

                // Game Setup Screen
                entry<GameSetupRoute> {
                    GameSetupEntryPoint(backStack = backStack) {
                        it?.let { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }

                // Score Sheet Screen
                entry<ScoreSheetRoute> {
                    ScoreSheetEntryPoint(backStack = backStack) {
                        it?.let { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }

                // Game End Screen
                entry<GameEndRoute> { route ->
                    GameEndEntryPoint(route.winnerName, route.winnerScore, backStack = backStack)
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}
