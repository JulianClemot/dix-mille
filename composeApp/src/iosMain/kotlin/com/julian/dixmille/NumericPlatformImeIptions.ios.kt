package com.julian.dixmille

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIImage
import platform.UIKit.UIToolbar

@OptIn(ExperimentalComposeUiApi::class)
actual fun numericPlatformImeOptions(onIosDone: () -> Unit, onIosCancel: () -> Unit): PlatformImeOptions? {
    return PlatformImeOptions {
        inputAccessoryView(
            value = DoneToolBar(onIosDone, onIosCancel)
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DoneToolBar(
    val onDone: () -> Unit,
    val onCancel: () -> Unit,
) : UIToolbar(frame = CGRectZero.readValue()) {
    init {
        translatesAutoresizingMaskIntoConstraints = false
        sizeToFit()

        val cancelButton = UIBarButtonItem(
            image = UIImage.systemImageNamed("xmark"),
            style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
            target = this,
            action = NSSelectorFromString(::handleCancel.name)
        )

        val flexSpace = UIBarButtonItem(
            barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace,
            target = null,
            action = null
        )

        val doneButton = UIBarButtonItem(
            image = UIImage.systemImageNamed("checkmark"),
            style = UIBarButtonItemStyle.UIBarButtonItemStyleDone,
            target = this,
            action = NSSelectorFromString(::handleDone.name)
        )

        setItems(listOf(cancelButton, flexSpace, doneButton), animated = false)
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleDone() = onDone()

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleCancel() = onCancel()
}
