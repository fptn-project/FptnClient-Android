package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.SniSpoofingMode
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassMethodsScreen(
    bypassMethod: BypassCensorshipMethod,
    sniSpoofingMode: SniSpoofingMode,
    sniText: String,
    sniCount: Int,
    serviceState: SniCheckerServiceState,
    progress: Pair<Int, Int>,
    currentCheckingSni: String,
    selectedServer: ServerEntity,
    allServers: List<ServerEntity>,
    onBypassMethodChange: (BypassCensorshipMethod) -> Unit,
    onSniSpoofingModeChange: (SniSpoofingMode) -> Unit,
    onEditSni: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onStartAutoSelect: (ServerEntity, Boolean) -> Unit,
    onStopAutoSelect: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var showEditSniDialog by remember { mutableStateOf(false) }
    var showAutoSelectDialog by remember { mutableStateOf(false) }

    if (showEditSniDialog) {
        EditSniDialog(
            currentSni = sniText,
            onSave = { newSni ->
                onEditSni(newSni)
                showEditSniDialog = false
            },
            onReset = {
                onEditSni(context.getString(R.string.default_sni))
                showEditSniDialog = false
            },
            onDismiss = { showEditSniDialog = false }
        )
    }

    if (showAutoSelectDialog) {
        AutoSelectDialog(
            servers = allServers,
            onStart = { server, reset ->
                onStartAutoSelect(server, reset)
                showAutoSelectDialog = false
            },
            onDismiss = {
                showAutoSelectDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Primary,
        bottomBar = {
            BottomNavBar(
                current = BottomNavTab.SETTINGS,
                onHome = onHome,
                onSettings = onSettings,
                context = context,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.bypass_methods_title),
                color = White,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.bypass_methods_info),
                color = White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            RadioMethodRow(
                label = stringResource(R.string.sni_reality_radio_button_label),
                selected = bypassMethod == BypassCensorshipMethod.SNI_REALITY,
                onClick = { onBypassMethodChange(BypassCensorshipMethod.SNI_REALITY) }
            )
            RadioMethodRow(
                label = stringResource(R.string.tls_handshake_obfuscation_radio_button_label),
                selected = bypassMethod == BypassCensorshipMethod.TLS_OBFUSCATION,
                onClick = { onBypassMethodChange(BypassCensorshipMethod.TLS_OBFUSCATION) }
            )

            if (bypassMethod == BypassCensorshipMethod.SNI_REALITY) {
                Spacer(modifier = Modifier.height(16.dp))

                SniSpoofingDropdown(
                    current = sniSpoofingMode,
                    onSelected = onSniSpoofingModeChange,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White10, RoundedCornerShape(12.dp))
                        .clickable { showEditSniDialog = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.your_current_sni),
                            color = White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = sniText,
                            color = Secondary,
                            fontSize = 16.sp,
                        )
                    }
                    Text(
                        text = stringResource(R.string.save_button),
                        color = Secondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.loaded_sni_label),
                            color = White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = sniCount.toString(),
                            color = Secondary,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (serviceState == SniCheckerServiceState.INACTIVE) {
                                showAutoSelectDialog = true
                            } else {
                                onStopAutoSelect()
                            }
                        },
                        enabled = sniCount > 0 || serviceState == SniCheckerServiceState.ACTIVE,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serviceState == SniCheckerServiceState.ACTIVE) Color(0xFFCC0000) else Secondary,
                            contentColor = Primary,
                        ),
                        shape = RoundedCornerShape(100.dp),
                    ) {
                        Text(
                            text = if (serviceState == SniCheckerServiceState.ACTIVE)
                                stringResource(R.string.stop_sni_checking_button_label)
                            else
                                stringResource(R.string.start_sni_checking_button_label)
                        )
                    }
                }

                if (serviceState == SniCheckerServiceState.ACTIVE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { if (progress.second > 0) progress.first.toFloat() / progress.second else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Secondary,
                        trackColor = White10,
                    )
                    Text(
                        text = "${progress.first}/${progress.second}",
                        color = White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (currentCheckingSni.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.checking_current_sni)} $currentCheckingSni",
                            color = White,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.selected_server) + ": " + selectedServer.getServerInfo(),
                        color = White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (serviceState != SniCheckerServiceState.ACTIVE) {
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
                        onClick = onSave,
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
    }
}

@Composable
private fun RadioMethodRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Secondary,
                unselectedColor = White,
            )
        )
        Text(text = label, color = White, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SniSpoofingDropdown(
    current: SniSpoofingMode,
    onSelected: (SniSpoofingMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = getSniModeName(context, current),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedContainerColor = White10,
                unfocusedContainerColor = White10,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTrailingIconColor = White,
                unfocusedTrailingIconColor = White,
            ),
            shape = RoundedCornerShape(12.dp),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Primary),
        ) {
            SniSpoofingMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(getSniModeName(context, mode), color = White) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    },
                    modifier = Modifier.background(Primary),
                )
            }
        }
    }
}

@Composable
private fun EditSniDialog(
    currentSni: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentSni) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Primary,
        title = { Text(stringResource(R.string.your_current_sni), color = White) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.sni_text_view_hint), color = White10) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = White10,
                    cursorColor = White,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.save_button), color = Secondary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reset_default_button), color = White)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button), color = White)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSelectDialog(
    servers: List<ServerEntity>,
    onStart: (ServerEntity, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedServer by remember { mutableStateOf(servers.firstOrNull()) }
    var resetChecked by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Primary,
        title = { Text(stringResource(R.string.select_server_for_autoselect), color = White) },
        text = {
            Column {
                if (servers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = selectedServer?.getServerInfo() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedContainerColor = White10,
                                unfocusedContainerColor = White10,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTrailingIconColor = White,
                                unfocusedTrailingIconColor = White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Primary),
                        ) {
                            servers.forEach { server ->
                                DropdownMenuItem(
                                    text = { Text(server.getServerInfo(), color = White) },
                                    onClick = {
                                        selectedServer = server
                                        expanded = false
                                    },
                                    modifier = Modifier.background(Primary),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = resetChecked,
                        onCheckedChange = { resetChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Secondary,
                            checkmarkColor = Primary,
                            uncheckedColor = White,
                        )
                    )
                    Text(
                        text = stringResource(R.string.reset_checked_previously_checkbox_label),
                        color = White,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedServer?.let { onStart(it, resetChecked) } },
                enabled = selectedServer != null,
            ) {
                Text(stringResource(R.string.start_sni_checking_button_label), color = Secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button), color = White)
            }
        }
    )
}

private fun getSniModeName(context: android.content.Context, mode: SniSpoofingMode): String {
    return when (mode) {
        SniSpoofingMode.SNI -> context.getString(R.string.sni)
        SniSpoofingMode.SNI_REALITY_CHROME_149 -> context.getString(R.string.sni_reality_radio_button_label_chrome_149)
        SniSpoofingMode.SNI_REALITY_CHROME_148 -> context.getString(R.string.sni_reality_radio_button_label_chrome_148)
        SniSpoofingMode.SNI_REALITY_CHROME_147 -> context.getString(R.string.sni_reality_radio_button_label_chrome_147)
        SniSpoofingMode.SNI_REALITY_CHROME_146 -> context.getString(R.string.sni_reality_radio_button_label_chrome_146)
        SniSpoofingMode.SNI_REALITY_CHROME_145 -> context.getString(R.string.sni_reality_radio_button_label_chrome_145)
        SniSpoofingMode.SNI_REALITY_FIREFOX_151 -> context.getString(R.string.sni_reality_radio_button_label_firefox_151)
        SniSpoofingMode.SNI_REALITY_FIREFOX_150 -> context.getString(R.string.sni_reality_radio_button_label_firefox_150)
        SniSpoofingMode.SNI_REALITY_FIREFOX_149 -> context.getString(R.string.sni_reality_radio_button_label_firefox_149)
        SniSpoofingMode.SNI_REALITY_YANDEX_26_4 -> context.getString(R.string.sni_reality_radio_button_label_yandex_26_4)
        SniSpoofingMode.SNI_REALITY_YANDEX_26_3 -> context.getString(R.string.sni_reality_radio_button_label_yandex_26_3)
        SniSpoofingMode.SNI_REALITY_YANDEX_25 -> context.getString(R.string.sni_reality_radio_button_label_yandex_25)
        SniSpoofingMode.SNI_REALITY_YANDEX_24 -> context.getString(R.string.sni_reality_radio_button_label_yandex_24)
        SniSpoofingMode.SNI_REALITY_SAFARI_26_5 -> context.getString(R.string.sni_reality_radio_button_label_safari_26_5)
        SniSpoofingMode.SNI_REALITY_SAFARI_26_4 -> context.getString(R.string.sni_reality_radio_button_label_safari_26_4)
        else -> mode.toString()
    }
}
