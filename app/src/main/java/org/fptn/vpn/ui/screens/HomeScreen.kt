package org.fptn.vpn.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10
import org.fptn.vpn.ui.theme.Yellow
import org.fptn.vpn.utils.CountryFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverList: List<ServerEntity>,
    selectedServer: ServerEntity?,
    connectionState: ConnectionState,
    statusText: String,
    errorText: String,
    timerText: String,
    downloadSpeed: String,
    uploadSpeed: String,
    connectedServerInfo: String,
    showPermissionWarning: Boolean,
    settingsEnabled: Boolean,
    onServerSelected: (ServerEntity) -> Unit,
    onConnectDisconnect: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isActive = connectionState.isActiveState()

    Scaffold(
        containerColor = Primary,
        bottomBar = {
            BottomNavBar(
                current = BottomNavTab.HOME,
                onHome = onHome,
                onSettings = { if (settingsEnabled) onSettings() },
                context = context,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
                .padding(padding)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission warning row (always occupies 80dp to avoid layout shift)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showPermissionWarning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.warning),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.permission_warning_text),
                            color = White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Timer row (always occupies 80dp to avoid layout shift)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isConnected) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.connection_time),
                            color = White,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = timerText,
                            color = White,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            // Toggle button (180x180dp)
            Image(
                painter = painterResource(
                    if (isActive) R.drawable.toggle_button_on else R.drawable.toggle_button_off
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .clickable { onConnectDisconnect() }
            )

            // Status text
            Text(
                text = statusText,
                color = Yellow,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )

            // Connected server info
            if (connectedServerInfo.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.server_label) + " ",
                        color = White,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = connectedServerInfo,
                        color = White,
                        fontSize = 14.sp,
                    )
                }
            }

            // Speed card with icons
            if (isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                        .background(White10, RoundedCornerShape(20.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 10.dp)
                        )
                        Text(
                            text = downloadSpeed,
                            color = White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                    // Upload
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uploadSpeed,
                            color = White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            textAlign = TextAlign.Start
                        )
                        Image(
                            painter = painterResource(R.drawable.upload),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(start = 10.dp)
                        )
                    }
                }
            }

            // Server spinner (when not active)
            if (!isActive && serverList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(5.dp))
                ServerDropdown(
                    servers = serverList,
                    selected = selectedServer,
                    onSelected = onServerSelected,
                )
            }

            // Error text
            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    color = Yellow,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerDropdown(
    servers: List<ServerEntity>,
    selected: ServerEntity?,
    onSelected: (ServerEntity) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = selected?.let { serverDisplayName(it) } ?: stringResource(R.string.select_server)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(52.dp),
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
            shape = RoundedCornerShape(20.dp),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Primary),
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = serverDisplayName(server),
                            color = White,
                        )
                    },
                    onClick = {
                        onSelected(server)
                        expanded = false
                    },
                    modifier = Modifier.background(Primary),
                )
            }
        }
    }
}

private fun serverDisplayName(server: ServerEntity): String {
    if (server.isAuto) return server.name
    val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode)
    val pingStr = when {
        server.pingMs > 0 -> " ${getPingEmoji(server.pingMs)} ${server.pingMs}ms"
        server.pingMs < 0 -> " ✗"
        else -> ""
    }
    return "${flag ?: ""} ${server.name}$pingStr"
}

private fun getPingEmoji(ping: Long) = when {
    ping < 150 -> "🟢"
    ping < 200 -> "🟡"
    ping < 300 -> "🟠"
    else -> "🔴"
}

private val ServerEntity.isAuto: Boolean
    get() = name == "Auto"
