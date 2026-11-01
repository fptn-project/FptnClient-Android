package org.fptn.vpn.ui.common

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.Secondary

private val TOKEN_CLIPBOARD_PREFIXES = listOf("fptn:", "fptnb:")

@Composable
fun MultilineTextInputDialog(
    title: String,
    hint: String,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onImportFromFile: (() -> Unit)? = null,
    importedText: String? = null,
    // When true (token dialog): show paste/clear buttons below the field, end-aligned.
    // When false (domain list dialog): no buttons, original behavior.
    showPasteClearIcons: Boolean = false,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }

    LaunchedEffect(importedText) {
        if (importedText != null) {
            text = importedText
        }
    }

    fun onPaste() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clipText = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.text?.toString()?.trim()
        if (clipText != null && TOKEN_CLIPBOARD_PREFIXES.any { clipText.startsWith(it) }) {
            text = clipText
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
                if (showPasteClearIcons) {
                    // Token mode: OutlinedTextField + paste/clear buttons below, end-aligned.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text(hint) },
                            minLines = 6,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { onPaste() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_content_paste_24),
                                    contentDescription = stringResource(R.string.paste_button),
                                    tint = Secondary,
                                )
                            }
                            IconButton(onClick = { text = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_close_24),
                                    contentDescription = stringResource(R.string.clear_button),
                                    tint = Secondary,
                                )
                            }
                        }
                    }
                } else {
                    // Default mode: plain OutlinedTextField (domain lists, etc.)
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(hint) },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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