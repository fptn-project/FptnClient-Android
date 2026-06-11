package org.fptn.vpn.views.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Lifecycle
import com.elvishew.xlog.XLog
import com.google.android.material.snackbar.Snackbar
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.services.vpn.FptnService
import org.fptn.vpn.services.vpn.FptnServiceState
import org.fptn.vpn.ui.screens.HomeScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.utils.PermissionsUtils
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.views.settings.SettingsActivity
import org.fptn.vpn.vpnclient.exception.ErrorCode
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger

class HomeActivity : ComponentActivity() {

    private val tag = "HomeActivity"
    private val viewModel: HomeActivityViewModel by viewModels()
    private var serviceConn: ServiceConnection? = null
    private var lastSelectedServer: ServerEntity? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result?.resultCode == RESULT_OK) {
            lastSelectedServer?.let { FptnService.startToConnect(this, it) }
        } else {
            Toast.makeText(this, R.string.vpn_permission_warning, Toast.LENGTH_SHORT).show()
            viewModel.errorTextLiveData.postValue(getString(R.string.vpn_permission_warning))
        }
    }

    private val requestedPermissions = AtomicInteger(0)

    private val settingsPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (requestedPermissions.decrementAndGet() == 0) {
            doConnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val serviceState by viewModel.serviceStateMutableLiveData.observeAsState(FptnServiceState.INITIAL)
            val serverList by viewModel.serverDtoListLiveData.observeAsState(emptyList())
            val timerText by viewModel.timerTextLiveData.observeAsState(getString(R.string.zero_time))
            val downloadSpeed by viewModel.downloadSpeedAsStringLiveData.observeAsState(getString(R.string.zero_speed))
            val uploadSpeed by viewModel.uploadSpeedAsStringLiveData.observeAsState(getString(R.string.zero_speed))
            val errorText by viewModel.errorTextLiveData.observeAsState("")
            val statusText by viewModel.statusTextLiveData.observeAsState(getString(R.string.disconnected))
            val connectedServerInfo by viewModel.connectedServerInfoLiveData.observeAsState("")

            val connectionState = serviceState?.connectionState ?: ConnectionState.DISCONNECTED
            val isConnected = connectionState == ConnectionState.CONNECTED
            val showPermissionWarning = isConnected && !PermissionsUtils.isAllOptionalPermissionsGranted(this)

            val currentSelectedServer = resolveSelectedServer(serverList ?: emptyList())

            FptnTheme {
                HomeScreen(
                    serverList = serverList ?: emptyList(),
                    selectedServer = currentSelectedServer,
                    connectionState = connectionState,
                    statusText = statusText ?: "",
                    errorText = errorText ?: "",
                    timerText = timerText ?: "",
                    downloadSpeed = downloadSpeed ?: "",
                    uploadSpeed = uploadSpeed ?: "",
                    connectedServerInfo = connectedServerInfo ?: "",
                    showPermissionWarning = showPermissionWarning,
                    settingsEnabled = !connectionState.isActiveState(),
                    onServerSelected = { server ->
                        lastSelectedServer = server
                        markServerSelected(server, serverList ?: emptyList())
                    },
                    onConnectDisconnect = {
                        val state = serviceState?.connectionState ?: ConnectionState.DISCONNECTED
                        if (state == ConnectionState.DISCONNECTED) {
                            lastSelectedServer = currentSelectedServer
                            onClickConnect()
                        } else if (state.isActiveState()) {
                            FptnService.startToDisconnect(this)
                        }
                    },
                    onHome = { /* already here */ },
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                )
            }
        }

        // observe exception for snackbar (must run on Activity, not Compose)
        viewModel.serviceStateMutableLiveData.observe(this) { state ->
            handleException(state)
        }

        requestAddTileService()
    }

    override fun onStart() {
        super.onStart()
        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                XLog.tag(tag).i("VPN service connected")
                val binder = service as FptnService.LocalBinder
                viewModel.subscribeService(binder.service)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                viewModel.unsubscribe()
            }
        }
        FptnService.bindService(this, serviceConn)
    }

    override fun onStop() {
        super.onStop()
        try { serviceConn?.let { unbindService(it) } } catch (e: Exception) {
            XLog.tag(tag).e("Error unbinding VPN service: %s", e.message)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopCheckingPing()
    }

    override fun onResume() {
        super.onResume()
        val state = viewModel.serviceStateMutableLiveData.value?.connectionState
        if (state == ConnectionState.DISCONNECTED) {
            viewModel.startCheckingPing()
        }
    }

    private fun resolveSelectedServer(servers: List<ServerEntity>): ServerEntity? {
        if (servers.isEmpty()) return null
        if (SharedPrefUtils.getResetSelectedServerEnabled(this)) return servers.first()
        return servers.firstOrNull { it.isSelected } ?: servers.first()
    }

    private fun markServerSelected(server: ServerEntity, all: List<ServerEntity>) {
        all.forEach { it.isSelected = it.id == server.id }
    }

    private fun onClickConnect() {
        if (!PermissionsUtils.checkNotificationEnabled(this)) {
            Toast.makeText(this, R.string.notifications_request_title, Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }

        if (!SharedPrefUtils.isPermissionsRequested(this)) {
            SharedPrefUtils.savePermissionsRequested(this, true)
            requestRequiredPermissions()
            return
        }

        doConnect()
    }

    private fun doConnect() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            viewModel.serviceStateMutableLiveData.postValue(FptnServiceState.FAKE_CONNECTING)
            FptnService.startToConnect(this, lastSelectedServer ?: resolveSelectedServer(viewModel.serverDtoListLiveData.value ?: emptyList()))
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestRequiredPermissions() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_request_title))
            .setMessage(getString(R.string.permission_request_text))
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                if (!PermissionsUtils.checkBatteryOptimizations(this)) {
                    requestedPermissions.incrementAndGet()
                    startActivityWithSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                }
                if (!PermissionsUtils.checkBackgroundDataTransferRestrictions(this)) {
                    requestedPermissions.incrementAndGet()
                    startActivityWithSettings(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS)
                }
                if (requestedPermissions.get() == 0) doConnect()
            }
            .setNegativeButton(getString(R.string.deny)) { _, _ -> doConnect() }
            .show()
    }

    private fun startActivityWithSettings(action: String) {
        settingsPermLauncher.launch(Intent(action).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun handleException(state: FptnServiceState?) {
        val exception = state?.exception ?: return
        if (!ErrorCode.isNeedToOfferRefreshToken(exception.errorCode)) return

        val errorText = viewModel.errorTextLiveData.value ?: ErrorCode.UNKNOWN_ERROR.value
        // Snackbar needs a View - use window decorView
        val rootView = window.decorView.rootView
        val snackbar = Snackbar.make(rootView, errorText, 8000)
        if (ErrorCode.isNeedToOfferRefreshToken(exception.errorCode)) {
            snackbar.setAction(getString(R.string.refresh_token)) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram_bot_link))))
            }
        }
        snackbar.show()
    }

    @SuppressLint("WrongConstant", "InlinedApi")
    private fun requestAddTileService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (SharedPrefUtils.isQuickSettingsTileRequested(this)) return

        val statusBarManager = getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
        try {
            statusBarManager.requestAddTileService(
                ComponentName(this, FptnTileService::class.java),
                "FPTN",
                Icon.createWithResource(this, R.drawable.ic_logo),
                mainExecutor
            ) { resultCode ->
                when (resultCode) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            XLog.tag(tag).e("Failed to request tile: %s", e.message)
            Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show()
        }
        SharedPrefUtils.saveQuickSettingsTileRequested(this, true)
    }
}
