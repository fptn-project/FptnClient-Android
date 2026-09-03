package org.fptn.vpn.ui.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.utils.backup.SettingsBackupManager
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "BackupSettingsScreen"

/**
 * Compose port of the legacy `BackupSettingsActivity` / `settings_backup_layout.xml`.
 * `SettingsBackupManager`'s export/import logic (and its exact JSON schema/versioning rules)
 * is untouched; only the SAF document picker + share-sheet wiring moved from
 * `ActivityResultLauncher` fields to `rememberLauncherForActivityResult`.
 */
@Composable
fun BackupSettingsScreen() {
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf<Uri?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            writeBackupToUri(context, uri)
        }
    }
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            showRestoreConfirm = uri
        }
    }

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
                .padding(bottom = 8.dp),
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
                text = stringResource(R.string.backups_title),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 10.dp),
            )

            // Save settings to file
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.backup_save_title),
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.backup_save_description),
                    color = White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LegacyPillButton(
                        text = stringResource(R.string.backup_save_button),
                        backgroundDrawable = R.drawable.round_back_secondary_100,
                        textColor = Primary,
                        bold = true,
                        contentPadding = 12.dp,
                        onClick = { onSaveToFile(createFileLauncher) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    LegacyPillButton(
                        text = stringResource(R.string.backup_share_button),
                        backgroundDrawable = R.drawable.round_back_secondary_cancel_100,
                        textColor = Primary,
                        bold = true,
                        contentPadding = 12.dp,
                        onClick = { onShare(context) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                }
            }

            // Restore settings from file
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.backup_restore_title),
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.backup_restore_description),
                    color = White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                LegacyPillButton(
                    text = stringResource(R.string.backup_restore_button),
                    backgroundDrawable = R.drawable.round_back_secondary_100,
                    textColor = Primary,
                    bold = true,
                    contentPadding = 12.dp,
                    onClick = { onLoadFromFile(openFileLauncher) },
                )
            }
        }

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.HOME)) },
            onNavigateSettings = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.SETTINGS)) },
            onShare = { showShareDialog = true },
        )
    }

    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = null
                    restoreFromUri(context, uri)
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

private fun onSaveToFile(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, generateBackupFileName())
    }
    launcher.launch(intent)
}

private fun onLoadFromFile(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        // Downloaded/shared *.json files often come with a generic mime type
        type = "*/*"
    }
    launcher.launch(intent)
}

private fun writeBackupToUri(context: android.content.Context, uri: Uri) {
    Thread {
        try {
            val json = SettingsBackupManager.exportToJson(context)
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
            }
            XLog.tag(TAG).i("Settings backup saved [uri=%s]", uri)
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, R.string.backup_saved_success, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            XLog.tag(TAG).e("Failed to save settings backup: %s", e.message)
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, R.string.backup_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}

private fun onShare(context: android.content.Context) {
    Thread {
        try {
            val json = SettingsBackupManager.exportToJson(context)

            val shareDir = File(context.cacheDir, "backups")
            shareDir.mkdirs()
            val backupFile = File(shareDir, generateBackupFileName())
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
            }

            val contentUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", backupFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            XLog.tag(TAG).i("Sharing settings backup [file=%s]", backupFile.name)
            ContextCompat.getMainExecutor(context).execute {
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.backup_share_button)))
            }
        } catch (e: Exception) {
            XLog.tag(TAG).e("Failed to share settings backup: %s", e.message)
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, R.string.backup_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}

private fun restoreFromUri(context: android.content.Context, uri: Uri) {
    Thread {
        try {
            val json = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri), StandardCharsets.UTF_8))
                .use { it.lineSequence().joinToString("\n") }
            SettingsBackupManager.importFromJson(context, json)
            XLog.tag(TAG).i("Settings restored from backup [uri=%s]", uri)
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, R.string.backup_restore_success, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            XLog.tag(TAG).e("Failed to restore settings backup: %s", e.message)
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, R.string.backup_restore_error, Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}

private fun generateBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "fptn-settings-$timestamp.json"
}
