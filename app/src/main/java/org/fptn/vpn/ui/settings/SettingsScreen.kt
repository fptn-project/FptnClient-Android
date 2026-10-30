package org.fptn.vpn.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LegacySpinner
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Gray
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.utils.PermissionsUtils
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.XLogInitializer
import androidx.compose.ui.platform.LocalConfiguration

private const val TAG = "SettingsScreen"

/**
 * Compose port of the legacy `SettingsActivity` / `settings_layout.xml`. Reuses
 * [SettingsViewModel] unchanged. Every sub-screen it links to (Update Token, Bypass Methods,
 * Per-App VPN Mode, Experimental Settings, Logs, Backup, Home) is a Compose destination in the
 * same nav graph, so those rows navigate straight through the passed-in callbacks.
 */
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateUpdateToken: () -> Unit,
    onNavigateBypassMethods: () -> Unit,
    onNavigatePerAppVpnMode: () -> Unit,
    onNavigateExperimentalSettings: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateTokenManagement: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTv = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_TELEVISION != 0
    val lifecycleOwner = LocalLifecycleOwner.current

    var showShareDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showClearLogsConfirm by remember { mutableStateOf(false) }
    var sponsorsExpanded by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showBackgroundDataDialog by remember { mutableStateOf(false) }

    var batteryOptimizationGranted by remember { mutableStateOf(PermissionsUtils.checkBatteryOptimizations(context)) }
    var backgroundDataGranted by remember { mutableStateOf(PermissionsUtils.checkBackgroundDataTransferRestrictions(context)) }

    // Capture the screen-level LocalDensity ONCE, before any AlertDialog is composed. AlertDialog
    // renders in a separate window (androidx.compose.ui.window.Dialog) that does NOT inherit
    // CompositionLocals from its parent, so reading LocalDensity.current INSIDE the dialog's
    // title/text/button lambdas returns the system Density (without our custom fontScale from
    // MainActivity). Providing dialogDensity explicitly fixes both "font size doesn't change
    // when the user picks a custom font scale" and "line height looks wrong".
    val dialogDensity = LocalDensity.current

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            XLog.tag(TAG).e("Failed to read app version: %s", e.message)
            ""
        }
    }

    // Re-check permission state whenever the user comes back from the system settings screen,
    // exactly like the legacy Activity's onResume override.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationGranted = PermissionsUtils.checkBatteryOptimizations(context)
                backgroundDataGranted = PermissionsUtils.checkBackgroundDataTransferRestrictions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background)
            // Excludes the bottom side deliberately: BottomNavBar consumes the navigation-bar
            // inset itself so its background reaches the real screen edge instead of floating
            // above it with the screen background showing through underneath (see
            // BottomNavBar's window insets listener).
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.icon_settings_circle_100),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .size(80.dp),
            )
            Text(
                text = appVersion,
                color = White,
                modifier = Modifier.padding(top = 10.dp),
            )

            SettingsCard {
                HtmlLinkText(
                    html = stringResource(R.string.info_message_html),
                    color = White,
                    bold = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsNavRow(
                icon = R.drawable.ic_baseline_key_24,
                title = stringResource(R.string.token_management_title),
                description = stringResource(R.string.token_management_description),
                onClick = onNavigateTokenManagement,
            )

            SettingsNavRow(
                icon = R.drawable.ic_baseline_update_24,
                title = stringResource(R.string.update_token_button),
                onClick = onNavigateUpdateToken,
            ) {
                HtmlLinkText(
                    html = stringResource(R.string.settings_token_info_html),
                    color = Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsNavRow(
                icon = R.drawable.ic_logo_24,
                title = stringResource(R.string.bypass_methods_title),
                description = stringResource(R.string.bypass_methods_info),
                onClick = onNavigateBypassMethods,
            )

            SettingsNavRow(
                icon = R.drawable.ic_per_app_vpn_mode,
                title = stringResource(R.string.per_app_vpn_settings_title),
                description = stringResource(R.string.per_app_vpn_mode_info),
                onClick = onNavigatePerAppVpnMode,
            )

            SettingsNavRow(
                icon = R.drawable.ic_experimental_features_24,
                title = stringResource(R.string.experimental_features_label),
                description = stringResource(R.string.experimental_features_info),
                onClick = onNavigateExperimentalSettings,
            )

            if (!isTv) {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.user_permissions_24),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.required_permissions),
                        color = White,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 4.dp)) {
                    PermissionRow(
                        icon = R.drawable.eco_battery_24,
                        label = stringResource(R.string.battery_optimization_request_dialog_title),
                        granted = batteryOptimizationGranted,
                        onRequest = { showBatteryOptimizationDialog = true },
                    )
                    PermissionRow(
                        icon = R.drawable.cloud_back_up_24,
                        label = stringResource(R.string.background_data_request_dialog_title),
                        granted = backgroundDataGranted,
                        onRequest = { showBackgroundDataDialog = true },
                    )
                }
                }
            }

            SettingsNavRow(
                icon = R.drawable.ic_logo_24,
                title = stringResource(R.string.logs),
                description = stringResource(R.string.view_and_copy_application_logs),
                onClick = onNavigateLogs,
            ) {
                Text(
                    text = stringResource(R.string.clear_logs_button),
                    color = Gray,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { showClearLogsConfirm = true },
                )
            }

            SettingsNavRow(
                icon = R.drawable.cloud_back_up_24,
                title = stringResource(R.string.backups_title),
                description = stringResource(R.string.backups_info),
                onClick = onNavigateBackup,
            )

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sponsorsExpanded = !sponsorsExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_sponsors_update_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = stringResource(R.string.sponsors_text) + if (sponsorsExpanded) "  ▲" else "  ▼",
                        color = White,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                if (sponsorsExpanded) {
                    HtmlLinkText(
                        html = stringResource(R.string.sponsors_usernames),
                        color = White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp),
                    )
                }
            }

            SettingsNavRow(
                icon = R.drawable.ic_baseline_logout_24,
                title = stringResource(R.string.logout_button),
                onClick = { showLogoutConfirm = true },
            )
        }

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = true,
            onNavigateHome = onNavigateHome,
            onNavigateSettings = {},
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        stringResource(R.string.dialog_logout_title),
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        stringResource(R.string.dialog_logout_message),
                        color = White,
                        fontSize = 14.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showLogoutConfirm = false
                        viewModel.deleteAllServers()
                        onLoggedOut()
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text(stringResource(R.string.no))
                    }
                }
            },
        )
    }

    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.battery_optimization_request_dialog_title))
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.battery_optimization_request_dialog_text))
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showBatteryOptimizationDialog = false
                        requestBatteryOptimizationExemption(context)
                    }) { Text(stringResource(R.string.grant)) }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showBatteryOptimizationDialog = false
                        XLog.tag(TAG).w("Battery optimization exemption denied by user")
                    }) { Text(stringResource(R.string.deny)) }
                }
            },
        )
    }

    if (showBackgroundDataDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundDataDialog = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.background_data_request_dialog_title))
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.background_data_request_dialog_text))
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showBackgroundDataDialog = false
                        requestBackgroundDataTransferPermission(context)
                    }) { Text(stringResource(R.string.grant)) }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showBackgroundDataDialog = false
                        XLog.tag(TAG).w("Background data transfer permission denied by user")
                    }) { Text(stringResource(R.string.deny)) }
                }
            },
        )
    }

    if (showClearLogsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirm = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.dialog_clear_logs_title))
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.dialog_clear_logs_message))
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showClearLogsConfirm = false
                        XLogInitializer.clearLogs(context)
                        Toast.makeText(context, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.yes)) }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = { showClearLogsConfirm = false }) { Text(stringResource(R.string.no)) }
                }
            },
        )
    }
}

@SuppressLint("BatteryLife")
private fun requestBatteryOptimizationExemption(context: android.content.Context) {
    // On MIUI the OS never reports the exemption back — record that we asked so the
    // toggle can settle to granted instead of always showing off.
    SharedPrefUtils.saveBatteryOptimizationRequested(context, true)
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = Uri.parse("package:" + context.packageName)
    context.startActivity(intent)
}

private fun requestBackgroundDataTransferPermission(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS)
    intent.data = Uri.parse("package:" + context.packageName)
    context.startActivity(intent)
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsNavRow(
    icon: Int,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = title,
                color = White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Image(
                painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        if (description != null) {
            Text(
                text = description,
                color = Gray,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
        content?.let {
            Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                it()
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: Int, label: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = White,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Switch(
            checked = granted,
            onCheckedChange = { if (!granted) onRequest() },
        )
    }
}
