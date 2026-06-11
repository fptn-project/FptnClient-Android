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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader(text = stringResource(R.string.your_servers))

            serverList.forEach { server ->
                ServerListItem(server = server)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsNavigationItem(
                icon = R.drawable.ic_baseline_update_24,
                title = stringResource(R.string.update_token_button),
                onClick = onUpdateToken,
            )

            SettingsNavigationItem(
                icon = R.drawable.ic_baseline_your_servers_24,
                title = stringResource(R.string.bypass_methods_title),
                onClick = onBypassMethods,
            )

            SettingsNavigationItem(
                icon = R.drawable.ic_per_app_vpn_mode,
                title = stringResource(R.string.per_app_vpn_mode_title),
                onClick = onPerAppVpn,
            )

            SettingsNavigationItem(
                icon = R.drawable.ic_experimental_features_24,
                title = stringResource(R.string.experimental_features_label),
                onClick = onExperimentalSettings,
            )

            SettingsNavigationItem(
                icon = R.drawable.ic_baseline_menu_24,
                title = stringResource(R.string.logs),
                onClick = onLogs,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(text = stringResource(R.string.required_permissions))

            PermissionSwitch(
                label = stringResource(R.string.battery_optimization_request_dialog_title),
                granted = batteryOptGranted,
                onClick = onBatteryOptClick,
            )

            PermissionSwitch(
                label = stringResource(R.string.background_data_request_dialog_title),
                granted = backgroundDataGranted,
                onClick = onBackgroundDataClick,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(text = stringResource(R.string.sponsors_text))

            HtmlText(
                html = sponsorsHtml,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textColor = White,
                textSizeSp = 14f,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HtmlText(
                html = tokenInfoHtml,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textColor = White,
                textSizeSp = 14f,
            )

            HtmlText(
                html = aboutHtml,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textColor = White,
                textSizeSp = 14f,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = appVersion,
                color = Hint,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsNavigationItem(
                icon = R.drawable.ic_baseline_logout_24,
                title = stringResource(R.string.logout_button),
                onClick = onLogout,
                tint = Color(0xFFCC0000),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Secondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Divider(color = White10, thickness = 1.dp)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ServerListItem(server: ServerEntity) {
    val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (flag != null) {
                Text(text = flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
            }
            Column {
                Text(text = server.name, color = White, fontSize = 14.sp)
                Text(text = server.host, color = Hint, fontSize = 12.sp)
            }
        }
    }
    Divider(color = White10, thickness = 0.5.dp)
}

@Composable
private fun SettingsNavigationItem(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    tint: Color = White,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 0.dp)
            )
            Text(
                text = title,
                color = tint,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_24),
            contentDescription = null,
            tint = White10,
            modifier = Modifier.size(16.dp)
        )
    }
    Divider(color = White10, thickness = 0.5.dp)
}

@Composable
private fun PermissionSwitch(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
