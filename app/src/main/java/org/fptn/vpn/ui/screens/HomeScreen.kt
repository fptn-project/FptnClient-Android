package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.theme.DeniedRed
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                color = White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    color = Yellow,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            ConnectButton(
                isActive = isActive,
                connectionState = connectionState,
                onClick = onConnectDisconnect,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isActive && serverList.isNotEmpty()) {
                ServerDropdown(
                    servers = serverList,
                    selected = selectedServer,
                    onSelected = onServerSelected,
                )
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))

                ConnectedInfoCard(
                    timerText = timerText,
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    connectedServerInfo = connectedServerInfo,
                )
            }

            if (showPermissionWarning) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_warning_text),
                    color = DeniedRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x37000000), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectButton(
    isActive: Boolean,
    connectionState: ConnectionState,
    onClick: () -> Unit,
) {
    val borderColor = if (isActive) Secondary else White10
    val innerColor = if (isActive) Secondary else White10

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .border(4.dp, borderColor, CircleShape)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(148.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = innerColor,
                contentColor = Primary,
            ),
        ) {
            Icon(
                painter = painterResource(
                    if (isActive) R.drawable.ic_baseline_logout_24
                    else R.drawable.ic_baseline_update_24
                ),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Primary,
            )
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
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

@Composable
private fun ConnectedInfoCard(
    timerText: String,
    downloadSpeed: String,
    uploadSpeed: String,
    connectedServerInfo: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White10, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "↓ $downloadSpeed", color = White, fontSize = 14.sp)
            }
            Text(
                text = timerText,
                color = Secondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "↑ $uploadSpeed", color = White, fontSize = 14.sp)
            }
        }
        if (connectedServerInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = connectedServerInfo,
                color = White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val ServerEntity.isAuto: Boolean
    get() = name == "Auto"
