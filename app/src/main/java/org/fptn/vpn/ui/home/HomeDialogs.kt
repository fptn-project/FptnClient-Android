package org.fptn.vpn.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.legacyDrawableBackground

/**
 * All the dialogs `HomeScreen` can show, extracted out of it so its own composable body stays
 * focused on wiring state and laying out the screen. Each dialog only takes the callbacks it
 * needs — closing itself and any follow-up navigation stay the caller's responsibility.
 */
@Composable
fun TokenReminderDialog(onUpdateToken: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = {}, // Not cancelable — the user must pick "Update" or "Later".
        title = { Text(stringResource(R.string.token_reminder_title)) },
        text = { HtmlLinkText(html = stringResource(R.string.token_reminder_message)) },
        confirmButton = {
            TextButton(onClick = onUpdateToken) { Text(stringResource(R.string.token_reminder_update)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.token_reminder_later)) }
        },
    )
}

@Composable
fun VpnSwitchDialog(onSwitch: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.vpn_switch_title)) },
        text = { Text(stringResource(R.string.vpn_switch_message)) },
        confirmButton = {
            TextButton(onClick = onSwitch) { Text(stringResource(R.string.vpn_switch_button)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel_button)) }
        },
    )
}

@Composable
fun VpnSetupErrorDialog(onOk: () -> Unit, onOpenVpnSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onOk,
        title = { Text(stringResource(R.string.vpn_setup_error_title)) },
        text = { Text(stringResource(R.string.vpn_setup_error_message)) },
        confirmButton = {
            TextButton(onClick = onOk) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onOpenVpnSettings) { Text(stringResource(R.string.open_vpn_settings)) }
        },
    )
}

@Composable
fun BackgroundSetupDialog(
    notificationsGranted: Boolean,
    batteryGranted: Boolean,
    pinDone: Boolean,
    isXiaomi: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestBattery: () -> Unit,
    onOpenPin: () -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_setup_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.background_setup_text),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                SetupChecklistRow(
                    icon = R.drawable.bell_ring_24,
                    title = stringResource(R.string.background_setup_notifications_title),
                    done = notificationsGranted,
                    onClick = onRequestNotifications,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SetupChecklistRow(
                    icon = R.drawable.eco_battery_24,
                    title = stringResource(R.string.background_setup_battery_title),
                    description = stringResource(R.string.background_setup_battery_desc),
                    done = batteryGranted,
                    onClick = onRequestBattery,
                )
                if (isXiaomi) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SetupChecklistRow(
                        icon = R.drawable.ic_baseline_settings_24,
                        title = stringResource(R.string.background_setup_pin_title),
                        description = stringResource(R.string.background_setup_pin_desc),
                        done = pinDone,
                        onClick = onOpenPin,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone, enabled = notificationsGranted && batteryGranted) {
                Text(stringResource(R.string.background_setup_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text(stringResource(R.string.background_setup_later)) }
        },
    )
}

@Composable
private fun SetupChecklistRow(icon: Int, title: String, description: String? = null, done: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = title, fontSize = 14.sp)
            if (description != null) {
                Text(text = description, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
        Image(
            painter = painterResource(if (done) R.drawable.ic_check_16 else R.drawable.ic_outline_arrow_forward_ios_16),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun ConnectFailedHelpDialog(
    tokenIsStale: Boolean,
    onGetToken: () -> Unit,
    onBypass: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connect_failed_help_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        if (tokenIsStale) R.string.connect_failed_help_message_token_stale else R.string.connect_failed_help_message,
                    ),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                ConnectFailedHelpRow(text = stringResource(R.string.connect_failed_help_bypass), onClick = onBypass)
                Spacer(modifier = Modifier.height(8.dp))
                ConnectFailedHelpRow(text = stringResource(R.string.connect_failed_help_get_token), onClick = onGetToken)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.connect_failed_help_skip)) }
        },
    )
}

@Composable
private fun ConnectFailedHelpRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
