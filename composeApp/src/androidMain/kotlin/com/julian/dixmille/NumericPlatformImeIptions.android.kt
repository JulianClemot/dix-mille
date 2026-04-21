package com.julian.dixmille

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions


@OptIn(ExperimentalComposeUiApi::class)
actual fun numericPlatformImeOptions(onIosDone: () -> Unit, onIosCancel: () -> Unit): PlatformImeOptions? = null