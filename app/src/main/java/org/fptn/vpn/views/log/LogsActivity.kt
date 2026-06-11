package org.fptn.vpn.views.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.fptn.vpn.R
import org.fptn.vpn.ui.screens.LogsScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.settings.SettingsActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Arrays

class LogsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logs = loadLogs()
        setContent {
            FptnTheme {
                LogsScreen(
                    logsText = logs,
                    onCopyLogs = { copyLogs(logs) },
                    onHome = {
                        startActivity(Intent(this, HomeActivity::class.java))
                    },
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                )
            }
        }
    }

    private fun copyLogs(text: String) {
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.logs_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("logs", text))
        Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show()
    }

    private fun loadLogs(): String {
        val logDir = File(filesDir, "logs2")
        if (!logDir.exists() || !logDir.isDirectory) return "No logs directory."
        val files = logDir.listFiles() ?: return "No log files."
        if (files.isEmpty()) return "No log files."

        val latest = files
            .filter { it.isFile && it.canRead() }
            .maxByOrNull { it.lastModified() }
            ?: return "No readable log file."

        return try {
            val sb = StringBuilder()
            BufferedReader(FileReader(latest)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
            }
            if (sb.isEmpty()) "Log file is empty." else sb.toString()
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }
}
