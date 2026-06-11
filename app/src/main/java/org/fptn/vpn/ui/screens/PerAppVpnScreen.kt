package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.fptn.vpn.R
import org.fptn.vpn.enums.PerAppVpnMode
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.theme.GrantedGreen
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10
import org.fptn.vpn.views.perappvpn.AppInfo
import androidx.compose.foundation.Image

@Composable
fun PerAppVpnScreen(
    mode: PerAppVpnMode,
    appList: List<AppInfo>,
    isLoading: Boolean,
    showSystemApps: Boolean,
    onModeChange: (PerAppVpnMode) -> Unit,
    onAppToggle: (AppInfo) -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(appList, searchQuery, mode) {
        appList
            .filter { app ->
                searchQuery.isEmpty() ||
                        app.label.lowercase().contains(searchQuery.lowercase())
            }
            .sortedWith(
                compareByDescending<AppInfo> { app ->
                    when (mode) {
                        PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed
                        PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed
                        else -> false
                    }
                }.thenBy { it.label }
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
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.per_app_vpn_settings_title),
                    color = White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.per_app_vpn_mode_info),
                    color = White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PerAppModeRadioRow(
                    label = stringResource(R.string.all_app_mode_radio_button_label),
                    selected = mode == PerAppVpnMode.OFF,
                    onClick = { onModeChange(PerAppVpnMode.OFF) }
                )
                PerAppModeRadioRow(
                    label = stringResource(R.string.only_allowed_apps_mode_radio_button_label),
                    selected = mode == PerAppVpnMode.ONLY_ALLOWED,
                    onClick = { onModeChange(PerAppVpnMode.ONLY_ALLOWED) }
                )
                PerAppModeRadioRow(
                    label = stringResource(R.string.disallowed_apps_radio_button_label),
                    selected = mode == PerAppVpnMode.EXCEPT_DISALLOWED,
                    onClick = { onModeChange(PerAppVpnMode.EXCEPT_DISALLOWED) }
                )
            }

            if (mode != PerAppVpnMode.OFF) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(stringResource(R.string.search_apps_hint), color = White10)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedContainerColor = White10,
                            unfocusedContainerColor = White10,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = White,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.show_system_apps_label),
                            color = White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showSystemApps,
                            onCheckedChange = onShowSystemAppsChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Primary,
                                checkedTrackColor = GrantedGreen,
                                uncheckedThumbColor = White,
                                uncheckedTrackColor = White10,
                            )
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Secondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(
                            items = filteredApps,
                            key = { it.packageName }
                        ) { app ->
                            AppItemRow(
                                app = app,
                                mode = mode,
                                onToggle = { onAppToggle(app) }
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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

@Composable
private fun PerAppModeRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun AppItemRow(
    app: AppInfo,
    mode: PerAppVpnMode,
    onToggle: () -> Unit,
) {
    val isChecked = when (mode) {
        PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed
        PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed
        else -> false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app.icon != null) {
            Image(
                painter = rememberDrawablePainter(app.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
        } else {
            Box(modifier = Modifier.size(40.dp).padding(end = 8.dp))
        }
        Text(
            text = app.label,
            color = White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Secondary,
                uncheckedThumbColor = White,
                uncheckedTrackColor = White10,
            )
        )
    }
    Divider(color = White10, thickness = 0.5.dp)
}
