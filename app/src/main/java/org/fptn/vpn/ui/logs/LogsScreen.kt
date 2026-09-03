package org.fptn.vpn.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.White
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Compose port of the legacy `LogsActivity` / `logs_layout.xml`: a read-only viewer for the
 * most recently modified file under `getFilesDir()/logs2`, tap-to-copy to the clipboard.
 */
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val logs = remember { loadLogs(context) }
    var showShareDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_24),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 30.dp)
                    .size(80.dp),
            )

            Text(
                text = stringResource(R.string.logs),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp),
            )

            Text(
                text = logs,
                color = White,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { copyLogs(context, logs) }
                    .background(Color(0xFF1A1A1A))
                    .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 8.dp),
            )
        }

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.HOME)) },
            onNavigateSettings = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.SETTINGS)) },
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

private fun copyLogs(context: Context, text: String) {
    if (text.isEmpty()) {
        Toast.makeText(context, R.string.logs_empty, Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("logs", text))
    Toast.makeText(context, R.string.logs_copied, Toast.LENGTH_SHORT).show()
}

private fun loadLogs(context: Context): String {
    val logDir = File(context.filesDir, "logs2")
    if (!logDir.exists() || !logDir.isDirectory) {
        return "No logs directory."
    }
    val files = logDir.listFiles()
    if (files == null || files.isEmpty()) {
        return "No log files."
    }
    val latest = files
        .filter { it.isFile && it.canRead() }
        .maxByOrNull { it.lastModified() }
        ?: return "No readable log file."

    val sb = StringBuilder()
    try {
        BufferedReader(FileReader(latest)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        }
    } catch (e: Exception) {
        return "Error reading log: ${e.message}"
    }
    return if (sb.isEmpty()) "Log file is empty." else sb.toString()
}
