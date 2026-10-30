package org.fptn.vpn.ui.bypassmethod

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.ui.common.ServerDropdown
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

private const val TAG = "SniDialogs"
private const val MIN_SNI_SUGGESTION_LENGTH = 3

@Composable
fun AutoSelectSniDialog(
    servers: List<ServerEntity>,
    onCancel: () -> Unit,
    onStart: (ServerEntity, Boolean) -> Unit,
) {
    var selected by remember(servers) { mutableStateOf(servers.firstOrNull() ?: ServerEntity.AUTO) }
    var resetChecked by remember { mutableStateOf(false) }

    val dialogDensity = LocalDensity.current
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
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
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                TextButton(onClick = { onStart(selected, resetChecked) }) {
                    Text(stringResource(R.string.start_sni_checking_button_label))
                }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(android.R.string.cancel))
                }
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
        if (text.length < MIN_SNI_SUGGESTION_LENGTH) {
            emptyList()
        } else {
            suggestions.filter { it.contains(text, ignoreCase = true) }.take(5)
        }
    }

    val dialogDensity = LocalDensity.current
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            CompositionLocalProvider(
                LocalDensity provides dialogDensity,
                LocalLayoutDirection provides LayoutDirection.Ltr // Force LTR for the whole dialog content
            ) {
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
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.heightIn(max = 250.dp),
                    ) {
                        CompositionLocalProvider(
                            LocalDensity provides dialogDensity,
                            LocalLayoutDirection provides LayoutDirection.Ltr
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
                }
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                TextButton(onClick = {
                    onSave(text)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.save_button))
                }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalDensity provides dialogDensity) {
                TextButton(onClick = {
                    onResetDefault()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.reset_default_button))
                }
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