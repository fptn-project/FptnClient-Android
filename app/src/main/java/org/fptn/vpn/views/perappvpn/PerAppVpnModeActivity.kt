package org.fptn.vpn.views.perappvpn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elvishew.xlog.XLog
import org.fptn.vpn.enums.PerAppVpnMode
import org.fptn.vpn.ui.screens.PerAppVpnScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.settings.SettingsActivity

class PerAppVpnModeActivity : ComponentActivity() {

    private val tag = "PerAppVpnModeActivity"
    private val viewModel: PerAppVpnModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadInstalledApps(packageManager)

        setContent {
            val mode by viewModel.perAppVpnModeMutableLiveData.observeAsState(PerAppVpnMode.OFF)
            val appList by viewModel.appListMutableLiveData.observeAsState(emptyList())
            var showSystemApps by remember { mutableStateOf(false) }
            val isLoading = appList.isEmpty()

            FptnTheme {
                PerAppVpnScreen(
                    mode = mode ?: PerAppVpnMode.OFF,
                    appList = appList ?: emptyList(),
                    isLoading = isLoading,
                    showSystemApps = showSystemApps,
                    onModeChange = { viewModel.setPerAppVpnMode(it) },
                    onAppToggle = { app ->
                        when (mode) {
                            PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed = !app.isAllowed
                            PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed = !app.isDisallowed
                            else -> Unit
                        }
                        // Trigger recomposition by posting the same list
                        viewModel.appListMutableLiveData.postValue(
                            viewModel.appListMutableLiveData.value
                        )
                    },
                    onShowSystemAppsChange = { show ->
                        showSystemApps = show
                        viewModel.setShowSystemApps(show)
                    },
                    onSave = {
                        XLog.tag(tag).i("Per-app VPN mode saved [mode=%s]", mode)
                        viewModel.saveAllSettings()
                        finish()
                    },
                    onCancel = {
                        XLog.tag(tag).i("Per-app VPN mode changes cancelled")
                        finish()
                    },
                    onHome = { startActivity(Intent(this, HomeActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }
}
