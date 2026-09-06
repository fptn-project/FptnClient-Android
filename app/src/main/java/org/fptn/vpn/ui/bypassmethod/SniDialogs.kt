package org.fptn.vpn.ui.bypassmethod

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.ui.common.ServerDropdown
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

private const val TAG = "SniDialogs"

/**
 * The two dialogs `BypassMethodsScreen` uses to manage SNI values: picking a server to
 * auto-scan against, and manually editing the SNI text (with autocomplete suggestions loaded
 * from the bundled SNI lists). Extracted out of the screen file since this is a self-contained
 * "SNI picker" feature rather than part of the screen's own layout.
 */
@Composable
fun AutoSelectSniDialog(
    servers: List<ServerEntity>,
    onCancel: () -> Unit,
    onStart: (ServerEntity, Boolean) -> Unit,
) {
    var selected by remember(servers) { mutableStateOf(servers.firstOrNull() ?: ServerEntity.AUTO) }
    var resetChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column {
                Text(stringResource(R.string.select_server_for_autoselect), modifier = Modifier.padding(bottom = 16.dp))
                ServerDropdown(servers = servers, selected = selected, onSelect = { selected = it })
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { resetChecked = !resetChecked },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = resetChecked, onCheckedChange = { resetChecked = it })
                    Text(stringResource(R.string.reset_checked_previously_checkbox_label))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(selected, resetChecked) }) {
                Text(stringResource(R.string.start_sni_checking_button_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun EditSniDialog(
    initialSni: String,
    suggestions: List<String>,
    onSave: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialSni) }
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(text, suggestions) {
        if (text.isEmpty()) emptyList() else suggestions.filter { it.contains(text, ignoreCase = true) }.take(20)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        expanded = it.isNotEmpty()
                    },
                    label = { Text(stringResource(R.string.sni_text_view_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = expanded && filteredSuggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                text = suggestion
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(text)
                onDismiss()
            }) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onResetDefault()
                onDismiss()
            }) {
                Text(stringResource(R.string.reset_default_button))
            }
        },
    )
}

fun loadSniSuggestions(context: Context): List<String> {
    val result = mutableListOf<String>()
    if (Locale.getDefault().language == "ru") {
        result.addAll(readSniRawFile(context, R.raw.russia))
    }
    result.addAll(readSniRawFile(context, R.raw.global))
    return result
}

private fun readSniRawFile(context: Context, rawResId: Int): List<String> {
    val list = mutableListOf<String>()
    try {
        context.resources.openRawResource(rawResId).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        list.add(trimmed)
                    }
                }
            }
        }
    } catch (e: Exception) {
        XLog.tag(TAG).e("Failed to read SNI suggestions file: %s", e.message)
    }
    return list
}
