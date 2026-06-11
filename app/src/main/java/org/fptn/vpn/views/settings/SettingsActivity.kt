package org.fptn.vpn.views.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.ui.screens.SettingsScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.utils.PermissionsUtils
import org.fptn.vpn.views.bypassmethod.BypassMethodsActivity
import org.fptn.vpn.views.experimentalsettings.ExperimentalSettingsActivity
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.log.LogsActivity
import org.fptn.vpn.views.perappvpn.PerAppVpnModeActivity
import org.fptn.vpn.views.splash.SplashActivity
import org.fptn.vpn.views.updatetoken.UpdateTokenActivity

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    private val tag = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadServersList()

        setContent {
            val serverList by viewModel.serverDtoListLiveData.observeAsState(emptyList())
            var batteryGranted by remember { mutableStateOf(PermissionsUtils.checkBatteryOptimizations(this)) }
            var backgroundDataGranted by remember { mutableStateOf(PermissionsUtils.checkBackgroundDataTransferRestrictions(this)) }

            val appVersion = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                ""
            }

            FptnTheme {
                SettingsScreen(
                    serverList = serverList ?: emptyList(),
                    appVersion = appVersion,
                    aboutHtml = getString(R.string.info_message_html),
                    tokenInfoHtml = getString(R.string.settings_token_info_html),
                    sponsorsHtml = getString(R.string.sponsors_usernames),
                    batteryOptGranted = batteryGranted,
                    backgroundDataGranted = backgroundDataGranted,
                    onBatteryOptClick = { requestBatteryOptimisationPermission { batteryGranted = PermissionsUtils.checkBatteryOptimizations(this) } },
                    onBackgroundDataClick = { requestBackgroundDataTransferPermission { backgroundDataGranted = PermissionsUtils.checkBackgroundDataTransferRestrictions(this) } },
                    onUpdateToken = { startActivity(Intent(this, UpdateTokenActivity::class.java)) },
                    onBypassMethods = { startActivity(Intent(this, BypassMethodsActivity::class.java)) },
                    onPerAppVpn = { startActivity(Intent(this, PerAppVpnModeActivity::class.java)) },
                    onExperimentalSettings = { startActivity(Intent(this, ExperimentalSettingsActivity::class.java)) },
                    onLogs = { startActivity(Intent(this, LogsActivity::class.java)) },
                    onLogout = { confirmLogout() },
                    onHome = { startActivity(Intent(this, HomeActivity::class.java)) },
                    onSettings = { /* already here */ },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadServersList()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteAllServers()
                startActivity(Intent(this, SplashActivity::class.java))
            }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimisationPermission(onDone: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.battery_optimization_request_dialog_title))
            .setMessage(getString(R.string.battery_optimization_request_dialog_text))
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                onDone()
            }
            .setNegativeButton(getString(R.string.deny)) { _, _ ->
                XLog.tag(tag).w("Battery optimization exemption denied by user")
            }
            .show()
    }

    private fun requestBackgroundDataTransferPermission(onDone: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.background_data_request_dialog_title))
            .setMessage(getString(R.string.background_data_request_dialog_text))
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                val intent = Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                onDone()
            }
            .setNegativeButton(getString(R.string.deny)) { _, _ ->
                XLog.tag(tag).w("Background data transfer permission denied by user")
            }
            .show()
    }
}
