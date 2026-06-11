package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.GrantedGreen
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10

data class ExperimentalSettings(
    val reconnectOnNetworkChange: Boolean,
    val reconnectOnIpChange: Boolean,
    val attemptsProgress: Int,
    val delayProgress: Int,
    val resetServerOnDisconnect: Boolean,
    val resetServerOnException: Boolean,
)

@Composable
fun ExperimentalSettingsScreen(
    initial: ExperimentalSettings,
    showTileButton: Boolean,
    onRequestTile: () -> Unit,
    onSave: (ExperimentalSettings) -> Unit,
    onCancel: () -> Unit,
) {
    val attemptsValues = intArrayOf(5, 15, 35, Int.MAX_VALUE)

    var reconnectOnNetworkChange by remember { mutableStateOf(initial.reconnectOnNetworkChange) }
    var reconnectOnIpChange by remember { mutableStateOf(initial.reconnectOnIpChange) }
    var attemptsProgress by remember { mutableFloatStateOf(initial.attemptsProgress.toFloat()) }
    var delayProgress by remember { mutableFloatStateOf(initial.delayProgress.toFloat()) }
    var resetOnDisconnect by remember { mutableStateOf(initial.resetServerOnDisconnect) }
    var resetOnException by remember { mutableStateOf(initial.resetServerOnException) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.experimental_features_label),
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.experimental_features_info),
            color = White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Divider(color = White10)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.reconnect_on_change),
            color = Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitchRow(
            label = stringResource(R.string.reconnect_on_change_network_type),
            checked = reconnectOnNetworkChange,
            onCheckedChange = { reconnectOnNetworkChange = it },
        )
        SettingSwitchRow(
            label = stringResource(R.string.reconnect_on_change_ip_address),
            checked = reconnectOnIpChange,
            onCheckedChange = { reconnectOnIpChange = it },
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.reconnection_on_failure_attempts),
            color = Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val attemptsLabel = attemptsValues.getOrNull(attemptsProgress.toInt())
            ?.let { if (it == Int.MAX_VALUE) "∞" else it.toString() } ?: "∞"
        Text(
            text = "${stringResource(R.string.attempts_count)} $attemptsLabel",
            color = White,
            fontSize = 14.sp,
        )
        Slider(
            value = attemptsProgress,
            onValueChange = { attemptsProgress = it },
            valueRange = 0f..3f,
            steps = 2,
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        val delayLabel = "${delayProgress.toInt() + 1}s"
        Text(
            text = "${stringResource(R.string.delay_between_attempts)} $delayLabel",
            color = White,
            fontSize = 14.sp,
        )
        Slider(
            value = delayProgress,
            onValueChange = { delayProgress = it },
            valueRange = 0f..29f,
            steps = 28,
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_reset_server_title),
            color = Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitchRow(
            label = stringResource(R.string.settings_reset_server_on_disconnect),
            checked = resetOnDisconnect,
            onCheckedChange = { resetOnDisconnect = it },
        )
        if (!resetOnDisconnect) {
            SettingSwitchRow(
                label = stringResource(R.string.settings_reset_server_on_exception),
                checked = resetOnException,
                onCheckedChange = { resetOnException = it },
            )
        }

        if (showTileButton) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.quick_settings_tile),
                color = Secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestTile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White10,
                    contentColor = White,
                ),
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(stringResource(R.string.quick_settings_tile))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White10,
                    contentColor = White,
                ),
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(stringResource(R.string.cancel_button))
            }
            Button(
                onClick = {
                    onSave(
                        ExperimentalSettings(
                            reconnectOnNetworkChange = reconnectOnNetworkChange,
                            reconnectOnIpChange = reconnectOnIpChange,
                            attemptsProgress = attemptsProgress.toInt(),
                            delayProgress = delayProgress.toInt(),
                            resetServerOnDisconnect = resetOnDisconnect,
                            resetServerOnException = resetOnException,
                        )
                    )
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Secondary,
                    contentColor = Primary,
                ),
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(stringResource(R.string.save_button))
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = GrantedGreen,
                uncheckedThumbColor = White,
                uncheckedTrackColor = White10,
            )
        )
    }
    Divider(color = White10, thickness = 0.5.dp)
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Secondary,
    activeTrackColor = Secondary,
    inactiveTrackColor = White10,
)
