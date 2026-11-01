package org.fptn.vpn.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import org.fptn.vpn.R

@Composable
fun MultilineTextInputDialog(
    title: String,
    hint: String,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    // TV-only: when non-null, a third "Import from file" button is shown.
    onImportFromFile: (() -> Unit)? = null,
    // TV-only: when this changes (non-null), the text field is updated with the file content.
    importedText: String? = null,
) {
    var text by remember { mutableStateOf(initialText) }

    // Update text when a file is imported (TV only — onImportFromFile != null)
    LaunchedEffect(importedText) {
        if (importedText != null) {
            text = importedText
        }
    }

    val dialogDensity = LocalDensity.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                Text(title)
            }
        },
        text = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(hint) },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                Row {
                    if (onImportFromFile != null) {
                        TextButton(onClick = onImportFromFile) {
                            Text(stringResource(R.string.token_import_from_file))
                        }
                    }
                    TextButton(onClick = {
                        onSave(text)
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.save_button))
                    }
                }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        },
    )
}