package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.components.HtmlText
import org.fptn.vpn.ui.theme.GrantedGreen
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10
import org.fptn.vpn.utils.CountryFlags

@Composable
fun SettingsScreen(
    serverList: List<ServerEntity>,
    appVersion: String,
    aboutHtml: String,
    tokenInfoHtml: String,
    sponsorsHtml: String,
    batteryOptGranted: Boolean,
    backgroundDataGranted: Boolean,
    onBatteryOptClick: () -> Unit,
    onBackgroundDataClick: () -> Unit,
    onUpdateToken: () -> Unit,
    onBypassMethods: () -> Unit,
    onPerAppVpn: () -> Unit,
    onExperimentalSettings: () -> Unit,
    onLogs: () -> Unit,
    onLogout: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Settings icon at top
            Icon(
                painter = painterResource(R.drawable.icon_settings_circle_100),
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier
                    .size(80.dp)
                    .padding(top = 30.dp)
            )

            // Version
            Text(
                text = appVersion,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // About info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(White10, RoundedCornerShape(20.dp))
                    .padding(10.dp)
            ) {
                HtmlText(
                    html = aboutHtml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 5.dp, end = 20.dp, bottom = 10.dp),
                    textColor = White,
                    textSizeSp = 14f,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Update Token card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onUpdateToken() }
            ) {
                NavRowItem(
                    icon = R.drawable.ic_baseline_update_24,
                    title = stringResource(R.string.update_token_button),
                )
                HtmlText(
                    html = tokenInfoHtml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                    textColor = Hint,
                    textSizeSp = 12f,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bypass Methods card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onBypassMethods() }
            ) {
                NavRowItem(
                    icon = R.drawable.ic_logo_24,
                    title = stringResource(R.string.bypass_methods_title),
                )
                Text(
                    text = stringResource(R.string.bypass_methods_info),
                    color = Hint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Per-App VPN card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onPerAppVpn() }
            ) {
                NavRowItem(
                    icon = R.drawable.ic_per_app_vpn_mode,
                    title = stringResource(R.string.per_app_vpn_mode_title),
                )
                Text(
                    text = stringResource(R.string.per_app_vpn_mode_info),
                    color = Hint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Required Permissions card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Section header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.user_permissions_24),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier
                            .height(16.dp)
                            .padding(start = 0.dp, end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.required_permissions),
                        color = White,
                        fontSize = 14.sp,
                    )
                }

                // Battery optimization
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 6.dp, bottom = 4.dp)
                ) {
                    PermissionSwitchRow(
                        icon = R.drawable.eco_battery_24,
                        label = stringResource(R.string.battery_optimization_request_dialog_title),
                        granted = batteryOptGranted,
                        onClick = onBatteryOptClick,
                    )

                    PermissionSwitchRow(
                        icon = R.drawable.cloud_back_up_24,
                        label = stringResource(R.string.background_data_request_dialog_title),
                        granted = backgroundDataGranted,
                        onClick = onBackgroundDataClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Experimental Features card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onExperimentalSettings() }
            ) {
                NavRowItem(
                    icon = R.drawable.ic_experimental_features_24,
                    title = stringResource(R.string.experimental_features_label),
                )
                Text(
                    text = stringResource(R.string.experimental_features_info),
                    color = Hint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Your Servers card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_your_servers_24),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier
                            .height(32.dp)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.your_servers),
                        color = White,
                        fontSize = 14.sp,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                ) {
                    serverList.forEach { server ->
                        ServerListItem(server = server)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sponsors card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sponsors_update_24),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier
                            .height(32.dp)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.sponsors_text),
                        color = White,
                        fontSize = 14.sp,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp)
                ) {
                    HtmlText(
                        html = sponsorsHtml,
                        modifier = Modifier.fillMaxWidth(),
                        textColor = White,
                        textSizeSp = 14f,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logs card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onLogs() }
            ) {
                NavRowItem(
                    icon = R.drawable.ic_logo_24,
                    title = stringResource(R.string.logs),
                )
                Text(
                    text = stringResource(R.string.view_and_copy_application_logs),
                    color = Hint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logout card
            SettingsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onLogout() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_logout_24),
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier
                                .height(32.dp)
                                .padding(horizontal = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.logout_button),
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(White10, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        content()
    }
}

@Composable
private fun NavRowItem(icon: Int, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = White,
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 12.dp)
            )
            Text(
                text = title,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
            contentDescription = null,
            tint = White,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Composable
private fun PermissionSwitchRow(
    icon: Int,
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = White,
            modifier = Modifier
                .size(28.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = label,
            color = White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = granted,
            onCheckedChange = { if (!granted) onClick() },
            enabled = !granted,
            modifier = Modifier.padding(end = 2.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = GrantedGreen,
                uncheckedThumbColor = White,
                uncheckedTrackColor = White10,
            )
        )
    }
}

@Composable
private fun ServerListItem(server: ServerEntity) {
    val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flag != null) {
            Text(text = flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Column {
            Text(text = server.name, color = White, fontSize = 14.sp)
            Text(text = server.host, color = Hint, fontSize = 12.sp)
        }
    }
}
