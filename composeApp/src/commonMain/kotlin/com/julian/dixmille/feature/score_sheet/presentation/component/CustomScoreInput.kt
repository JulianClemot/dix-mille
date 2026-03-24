package com.julian.dixmille.feature.score_sheet.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.custom_score_input_add_button
import dixmille.composeapp.generated.resources.custom_score_input_error_invalid
import dixmille.composeapp.generated.resources.custom_score_input_error_multiple
import dixmille.composeapp.generated.resources.custom_score_input_error_positive
import dixmille.composeapp.generated.resources.custom_score_input_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * Compact custom score input styled to match the dark theme preset buttons.
 */
@Composable
fun CustomScoreInput(
    onScoreSubmit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val errorInvalid = stringResource(Res.string.custom_score_input_error_invalid)
    val errorPositive = stringResource(Res.string.custom_score_input_error_positive)
    val errorMultiple = stringResource(Res.string.custom_score_input_error_multiple)

    fun submit() {
        val score = text.toIntOrNull()
        when {
            score == null -> error = errorInvalid
            score <= 0 -> error = errorPositive
            score % 50 != 0 -> error = errorMultiple
            else -> {
                onScoreSubmit(score)
                text = ""
                error = null
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = {
                Text(
                    text = stringResource(Res.string.custom_score_input_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    submit()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            isError = error != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        OutlinedButton(
            onClick = {
                submit()
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            modifier = Modifier.height(42.dp),
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = stringResource(Res.string.custom_score_input_add_button),
                style = MaterialTheme.typography.labelMedium,
                color = if (text.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
