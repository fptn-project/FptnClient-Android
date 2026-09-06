package org.fptn.vpn.ui.home

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.services.vpn.FptnService
import org.fptn.vpn.services.vpn.FptnServiceState
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.ServerDropdown
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.findActivity
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.Yellow
import org.fptn.vpn.utils.PermissionsUtils
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.vpnclient.exception.ErrorCode

private const val TOKEN_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
private const val TOKEN_STALE_AGE_MS = 3L * 24 * 60 * 60 * 1000
private const val CONNECT_FAILURES_BEFORE_HELP = 2

/**
 * Compose port of the legacy `HomeActivity` / `home_layout.xml`. Reuses [HomeActivityViewModel]
 * unchanged. The connect button's vertical position (originally a ConstraintLayout bias tuned
 * per screen height) is approximated with a top spacer sized to the same bias fraction rather
 * than pulled in via `constraintlayout-compose`, matching how every other ported screen in this
 * app favors plain Compose layout primitives over new layout dependencies. Similarly the traffic
 * card no longer stretches to fill the remaining space down to the bottom nav — it just wraps its
 * content, which reads the same in practice since the card was always top-aligned within that
 * space unless the (rarely toggled) speed chart was showing.
 */
@Composable
fun HomeScreen(viewModel: HomeActivityViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val serviceState by viewModel.serviceStateMutableLiveData.observeAsState(FptnServiceState.INITIAL)
    val connectionState = serviceState.connectionState
    val serverEntities by viewModel.serverDtoListLiveData.observeAsState(emptyList())
    val timerText by viewModel.timerTextLiveData.observeAsState(stringResource(R.string.zero_time))
    val downloadSpeed by viewModel.downloadSpeedAsStringLiveData.observeAsState(stringResource(R.string.zero_speed))
    val uploadSpeed by viewModel.uploadSpeedAsStringLiveData.observeAsState(stringResource(R.string.zero_speed))
    val statusText by viewModel.statusTextLiveData.observeAsState(stringResource(R.string.disconnected))
    val connectedServerInfo by viewModel.connectedServerInfoLiveData.observeAsState(null)
    val downloadTraffic by viewModel.downloadTrafficLiveData.observeAsState("0 B")
    val uploadTraffic by viewModel.uploadTrafficLiveData.observeAsState("0 B")
    val speedSample by viewModel.speedSampleLiveData.observeAsState(null)

    var selectedServer by remember { mutableStateOf(ServerEntity.AUTO) }
    // Sticky "are we showing the connected layout" flag: flips on CONNECTED/DISCONNECTED/BLOCKED,
    // left untouched on transitional states (CONNECTING, RECONNECTING, ...) — mirrors the legacy
    // observer's `switch` with a no-op `default`, so e.g. a silent RECONNECTING episode keeps
    // showing the connected UI instead of flashing back to the server dropdown.
    var showConnectedUi by remember { mutableStateOf(false) }
    var previousConnectionState by remember { mutableStateOf<ConnectionState?>(null) }
    var isResumed by remember { mutableStateOf(false) }

    var notificationsGranted by remember { mutableStateOf(PermissionsUtils.checkNotificationEnabled(context)) }
    var batteryGranted by remember { mutableStateOf(PermissionsUtils.checkBatteryOptimizations(context)) }
    var showTrafficChart by remember { mutableStateOf(SharedPrefUtils.getShowTrafficChart(context)) }
    val isXiaomi = remember { PermissionsUtils.isXiaomi() }
    var visitedPin by remember { mutableStateOf(false) }
    val pinDone = !isXiaomi || visitedPin || SharedPrefUtils.isXiaomiPinDone(context)

    var showBackgroundSetupDialog by remember { mutableStateOf(false) }
    var connectAfterBackgroundSetup by remember { mutableStateOf(false) }
    var notificationPermanentlyDenied by remember { mutableStateOf(false) }

    var showTokenReminderDialog by remember { mutableStateOf(false) }
    var showVpnSwitchDialog by remember { mutableStateOf(false) }
    var showVpnSetupErrorDialog by remember { mutableStateOf(false) }
    var showConnectFailedHelpDialog by remember { mutableStateOf(false) }
    var connectFailedTokenStale by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    val needsBackgroundSetup = !notificationsGranted || !batteryGranted
    val showPermissionWarning = connectionState == ConnectionState.CONNECTED && needsBackgroundSetup
    val activeState = connectionState.isActiveState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FptnService.startToConnect(context, selectedServer)
        } else {
            showVpnSetupErrorDialog = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            val activity = context.findActivity()
            if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                notificationPermanentlyDenied = true
            }
        }
        notificationsGranted = PermissionsUtils.checkNotificationEnabled(context)
    }

    fun connectVpn() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            viewModel.serviceStateMutableLiveData.postValue(FptnServiceState.FAKE_CONNECTING)
            FptnService.startToConnect(context, selectedServer)
        }
    }

    fun proceedToVpnConnect() {
        if (PermissionsUtils.isAlwaysOnVpnEnabledByAnotherApp(context)) {
            showVpnSwitchDialog = true
            return
        }
        val tokenAge = System.currentTimeMillis() - SharedPrefUtils.getTokenUpdatedDate(context)
        if (tokenAge >= TOKEN_MAX_AGE_MS) {
            showTokenReminderDialog = true
            return
        }
        connectVpn()
    }

    fun onToggleConnectClick() {
        if (connectionState == ConnectionState.DISCONNECTED) {
            if (needsBackgroundSetup) {
                connectAfterBackgroundSetup = true
                visitedPin = false
                showBackgroundSetupDialog = true
                return
            }
            proceedToVpnConnect()
        } else if (connectionState.isActiveState()) {
            FptnService.startToDisconnect(context)
        }
    }

    fun requestNotifications() {
        if (PermissionsUtils.checkNotificationEnabled(context)) {
            notificationsGranted = true
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermanentlyDenied) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }

    // Service binding (mirrors onStart/onStop) and ping-check gating (mirrors onResume/onPause;
    // ping checking is only ever active while DISCONNECTED and the screen is in the foreground).
    DisposableEffect(lifecycleOwner) {
        var connection: ServiceConnection? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            val localBinder = service as FptnService.LocalBinder
                            viewModel.subscribeService(localBinder.service)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            viewModel.unsubscribe()
                        }
                    }.also { FptnService.bindService(context, it) }
                }
                Lifecycle.Event.ON_STOP -> {
                    connection?.let {
                        try {
                            context.unbindService(it)
                        } catch (e: Exception) {
                            // Already unbound (e.g. service process gone) — nothing to clean up.
                        }
                    }
                    connection = null
                }
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    notificationsGranted = PermissionsUtils.checkNotificationEnabled(context)
                    batteryGranted = PermissionsUtils.checkBatteryOptimizations(context)
                    showTrafficChart = SharedPrefUtils.getShowTrafficChart(context)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            connection?.let {
                try {
                    context.unbindService(it)
                } catch (e: Exception) {
                    // Already unbound — nothing to clean up.
                }
            }
        }
    }

    LaunchedEffect(connectionState, isResumed) {
        if (connectionState == ConnectionState.DISCONNECTED && isResumed) {
            viewModel.startCheckingPing()
        } else {
            viewModel.stopCheckingPing()
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.CONNECTED -> showConnectedUi = true
            ConnectionState.DISCONNECTED, ConnectionState.BLOCKED -> showConnectedUi = false
            else -> {}
        }
        // Re-pick the default server only on the edge into DISCONNECTED, not on every ping
        // refresh while already disconnected — otherwise a user's manual dropdown pick would
        // keep getting reset every 5 minutes as ping results stream in.
        if (connectionState == ConnectionState.DISCONNECTED && previousConnectionState != ConnectionState.DISCONNECTED) {
            if (serverEntities.isNotEmpty()) {
                selectedServer = if (SharedPrefUtils.getResetSelectedServerEnabled(context)) {
                    serverEntities[0]
                } else {
                    serverEntities.firstOrNull { it.isSelected() } ?: serverEntities[0]
                }
            }
        }
        previousConnectionState = connectionState
    }

    LaunchedEffect(serviceState) {
        val exception = serviceState.exception ?: return@LaunchedEffect
        when {
            exception.errorCode == ErrorCode.VPN_INTERFACE_ERROR -> showVpnSetupErrorDialog = true
            ErrorCode.isNeedToOfferRefreshToken(exception.errorCode) -> {
                val result = snackbarHostState.showSnackbar(
                    message = statusText,
                    actionLabel = context.getString(R.string.refresh_token),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.telegram_bot_link))))
                }
            }
            connectionState == ConnectionState.DISCONNECTED && ErrorCode.isServerUnreachable(exception.errorCode) -> {
                if (SharedPrefUtils.getConnectFailedHelpEnabled(context) &&
                    SharedPrefUtils.getConnectFailuresInRow(context) >= CONNECT_FAILURES_BEFORE_HELP
                ) {
                    SharedPrefUtils.saveConnectFailuresInRow(context, 0)
                    if (!showConnectFailedHelpDialog) {
                        connectFailedTokenStale = System.currentTimeMillis() -
                            SharedPrefUtils.getTokenUpdatedDate(context) > TOKEN_STALE_AGE_MS
                        showConnectFailedHelpDialog = true
                    }
                }
            }
        }
    }

    // Request the quick-settings tile once, same as the legacy Activity's onCreate.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !SharedPrefUtils.isQuickSettingsTileRequested(context)
        ) {
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                statusBarManager?.requestAddTileService(
                    ComponentName(context, FptnTileService::class.java),
                    "FPTN",
                    Icon.createWithResource(context, R.drawable.ic_logo),
                    context.mainExecutor,
                ) { resultCode ->
                    when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            Toast.makeText(context, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show()
            }
            SharedPrefUtils.saveQuickSettingsTileRequested(context, true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legacyDrawableBackground(R.drawable.application_background),
        ) {
            if (showPermissionWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .clickable {
                            connectAfterBackgroundSetup = false
                            visitedPin = false
                            showBackgroundSetupDialog = true
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.warning),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.permission_warning_text),
                        color = White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val screenHeightDp = LocalConfiguration.current.screenHeightDp
                val verticalBias = when {
                    screenHeightDp < 600 -> 0.10f
                    screenHeightDp < 700 -> 0.15f
                    else -> 0.25f
                }
                Spacer(modifier = Modifier.height((screenHeightDp * verticalBias).dp))

                // Always composed (never conditionally removed) so it reserves the same layout
                // space whether shown or not — otherwise the button below would jump down by the
                // timer's height the moment it appears on connect. The legacy ConstraintLayout
                // avoided this because the button's position came from its own bias against the
                // parent, independent of the timer view above it; a plain Column has no such
                // independence, so the space has to be reserved instead.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(if (showConnectedUi) 1f else 0f)
                        .then(if (showConnectedUi) Modifier else Modifier.semantics { hideFromAccessibility() }),
                ) {
                    Text(text = stringResource(R.string.connection_time), color = White)
                    Text(text = timerText, color = White, modifier = Modifier.padding(bottom = 4.dp))
                }

                Image(
                    painter = painterResource(if (activeState) R.drawable.toggle_button_on else R.drawable.toggle_button_off),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.toggle_button_size))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onToggleConnectClick() },
                )

                Text(
                    text = statusText,
                    color = Yellow,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )

                if (showConnectedUi) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        Text(text = stringResource(R.string.server_label), color = White, modifier = Modifier.padding(end = 5.dp))
                        Text(text = connectedServerInfo.orEmpty(), color = White, maxLines = 2)
                    }
                } else {
                    ServerDropdown(
                        servers = serverEntities,
                        selected = selectedServer,
                        onSelect = { selectedServer = it },
                        enabled = !activeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp),
                    )
                }

                if (showConnectedUi) {
                    TrafficCard(
                        downloadSpeed = downloadSpeed,
                        uploadSpeed = uploadSpeed,
                        downloadTraffic = downloadTraffic,
                        uploadTraffic = uploadTraffic,
                        showChart = showTrafficChart,
                        speedSample = speedSample,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }

            BottomNavBar(
                isHomeScreen = true,
                isSettingsScreen = false,
                onNavigateHome = {},
                onNavigateSettings = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.SETTINGS)) },
                onShare = { showShareDialog = true },
                settingsEnabled = !activeState,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }

    if (showTokenReminderDialog) {
        TokenReminderDialog(
            onUpdateToken = {
                showTokenReminderDialog = false
                context.startActivity(MainActivity.intentForRoute(context, AppRoute.UPDATE_TOKEN))
            },
            onLater = {
                showTokenReminderDialog = false
                connectVpn()
            },
        )
    }

    if (showVpnSwitchDialog) {
        VpnSwitchDialog(
            onSwitch = {
                showVpnSwitchDialog = false
                connectVpn()
            },
            onCancel = { showVpnSwitchDialog = false },
        )
    }

    if (showVpnSetupErrorDialog) {
        VpnSetupErrorDialog(
            onOk = { showVpnSetupErrorDialog = false },
            onOpenVpnSettings = {
                showVpnSetupErrorDialog = false
                context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
            },
        )
    }

    if (showBackgroundSetupDialog) {
        BackgroundSetupDialog(
            notificationsGranted = notificationsGranted,
            batteryGranted = batteryGranted,
            pinDone = pinDone,
            isXiaomi = isXiaomi,
            onRequestNotifications = { requestNotifications() },
            onRequestBattery = {
                SharedPrefUtils.saveBatteryOptimizationRequested(context, true)
                openBatteryOptimizationSettings(context)
            },
            onOpenPin = {
                visitedPin = true
                SharedPrefUtils.saveXiaomiPinDone(context, true)
                PermissionsUtils.openMiuiSecurityApp(context)
            },
            onDismiss = { showBackgroundSetupDialog = false },
            onDone = {
                showBackgroundSetupDialog = false
                if (connectAfterBackgroundSetup) {
                    proceedToVpnConnect()
                }
            },
        )
    }

    if (showConnectFailedHelpDialog) {
        ConnectFailedHelpDialog(
            tokenIsStale = connectFailedTokenStale,
            onGetToken = {
                showConnectFailedHelpDialog = false
                context.startActivity(MainActivity.intentForRoute(context, AppRoute.UPDATE_TOKEN))
            },
            onBypass = {
                showConnectFailedHelpDialog = false
                context.startActivity(MainActivity.intentForRoute(context, AppRoute.BYPASS_METHODS))
            },
            onDismiss = { showConnectFailedHelpDialog = false },
        )
    }
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        PermissionsUtils.openMiuiBackgroundSettings(context)
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    } catch (e: Exception) {
        PermissionsUtils.openMiuiBackgroundSettings(context)
    }
}

