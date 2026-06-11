package org.fptn.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White

@Composable
fun LogsScreen(
    logsText: String,
    onCopyLogs: () -> Unit,
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = logsText,
                color = White,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyLogs() }
            )
        }
    }
}
