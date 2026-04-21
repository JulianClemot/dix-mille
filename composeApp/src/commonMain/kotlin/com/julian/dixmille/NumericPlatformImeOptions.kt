package com.julian.dixmille

import androidx.compose.ui.text.input.PlatformImeOptions

expect fun numericPlatformImeOptions(onIosDone: () -> Unit, onIosCancel: () -> Unit): PlatformImeOptions?