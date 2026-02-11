package com.julian.dixmille.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Inline custom score input component.
 * 
 * Allows user to enter a custom score value directly in the UI
 * without opening a dialog.
 */
@Composable
fun CustomScoreInput(
    onScoreSubmit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                error = null
            },
            modifier = Modifier.weight(1f),
            label = { Text("Custom Score") },
            placeholder = { Text("Enter points") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    val score = text.toIntOrNull()
                    when {
                        score == null -> error = "Invalid number"
                        score <= 0 -> error = "Must be positive"
                        score % 50 != 0 -> error = "Must be a multiple of 50"
                        else -> {
                            onScoreSubmit(score)
                            text = ""
                            error = null
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                }
            ),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Button(
            onClick = {
                val score = text.toIntOrNull()
                when {
                    score == null -> error = "Invalid number"
                    score <= 0 -> error = "Must be positive"
                    score % 50 != 0 -> error = "Must be a multiple of 50"
                    else -> {
                        onScoreSubmit(score)
                        text = ""
                        error = null
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
            },
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add")
        }
    }
}
