package org.fptn.vpn.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.fptn.vpn.R

/**
 * The multi-line "edit a domain list" dialog shape shared by the split-tunnel and domain
 * blacklist settings (a plain multi-line text field, Save/Cancel), reproducing each legacy
 * `AlertDialog.Builder` with a custom `EditText` view exactly (6-10 visible lines, save on
 * positive button, dismiss-only on negative).
 */
@Composable
fun MultilineTextInputDialog(
    title: String,
    hint: String,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(hint) },
                minLines = 6,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
    )
}
