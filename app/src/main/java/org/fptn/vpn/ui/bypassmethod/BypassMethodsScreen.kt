package org.fptn.vpn.ui.bypassmethod

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.ConnectionStrategy
import org.fptn.vpn.enums.SniSpoofingMode
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.ServerDropdown
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.Gray
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.views.bypassmethod.BypassMethodsViewModel
import org.fptn.vpn.vpnclient.exception.PVNClientException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

private const val TAG = "BypassMethodsScreen"

/**
 * Compose port of the legacy `BypassMethodsActivity` / `settings_bypass_methods_layout.xml`.
 * Reuses [BypassMethodsViewModel] unchanged, including its `SniCheckerService` binding
 * contract; the bind/unbind timing (on start/stop, not just once) is reproduced with a
 * [LifecycleEventObserver] instead of `onStart`/`onStop` overrides.
 */
@Composable
fun BypassMethodsScreen(
    viewModel: BypassMethodsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val bypassMethod by viewModel.bypassCensorshipMethodMutableLiveData.observeAsState(BypassCensorshipMethod.TLS_OBFUSCATION)
    val connectionStrategy by viewModel.connectionStrategyMutableLiveData.observeAsState(ConnectionStrategy.DUAL_TUNNEL)
    val sniSpoofingMode by viewModel.sniSpoofingModeMutableLiveData.observeAsState(SniSpoofingMode.SNI)
    val currentSni by viewModel.sniMutableLiveData.observeAsState("")
    val sniCount by viewModel.sniCountLiveData.observeAsState(0)
    val serviceState by viewModel.serviceState.observeAsState(SniCheckerServiceState.INACTIVE)
    val currentCheckingSni by viewModel.currentCheckingSniInfo.observeAsState("")
    val currentProgress by viewModel.currentProgress.observeAsState(android.util.Pair(0, 1))
    val selectedCheckingServer by viewModel.selectedServer.observeAsState(ServerEntity.AUTO)
    val foundedSni by viewModel.foundedSniEvent.observeAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var autoSelectServers by remember { mutableStateOf<List<ServerEntity>?>(null) }
    var showEditSniDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            viewModel.loadDefaultSni()
        } catch (err: PVNClientException) {
            XLog.tag(TAG).w("Failed to load default SNI list: %s", err.errorMessage)
        }
    }

    LaunchedEffect(foundedSni) {
        foundedSni?.let { sni ->
            Toast.makeText(context, "Found SNI: $sni", Toast.LENGTH_LONG).show()
        }
    }

    // Bind/unbind SniCheckerService on start/stop, exactly like the legacy Activity's
    // onStart/onStop overrides.
    val currentViewModel = rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        var connection: ServiceConnection? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val conn = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val binder = service as SniCheckerService.LocalBinder
                            currentViewModel.value.subscribeService(binder.service)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            currentViewModel.value.unsubscribe()
                        }
                    }
                    connection = conn
                    SniCheckerService.bindService(context, conn)
                }
                Lifecycle.Event.ON_STOP -> {
                    connection?.let { try { context.unbindService(it) } catch (e: Exception) { XLog.tag(TAG).e("Error unbinding SNI checker service: %s", e.message) } }
                    connection = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            connection?.let { try { context.unbindService(it) } catch (e: Exception) { XLog.tag(TAG).e("Error unbinding SNI checker service: %s", e.message) } }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            try {
                viewModel.readFileContent(uri)
            } catch (e: PVNClientException) {
                Toast.makeText(context, e.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onAutoSelectClicked() {
        if (viewModel.serviceState.value == SniCheckerServiceState.INACTIVE) {
            Futures.addCallback(
                viewModel.getAllServers(),
                object : FutureCallback<List<ServerEntity>> {
                    override fun onSuccess(result: List<ServerEntity>) {
                        autoSelectServers = result
                    }

                    override fun onFailure(t: Throwable) {
                        XLog.tag(TAG).e("Failed to load server list for SNI auto-select: %s", t.message)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        } else {
            SniCheckerService.stopChecking(context)
        }
    }

    val showSniSection = bypassMethod == BypassCensorshipMethod.SNI_REALITY

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_24),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .size(80.dp),
            )
            Text(
                text = stringResource(R.string.bypass_methods_title),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )

            // Connection strategy card
            BypassCard {
                CardHeader(stringResource(R.string.connection_strategy_title))
                LegacySpinner(
                    items = ConnectionStrategy.entries,
                    selected = connectionStrategy,
                    label = { connectionStrategyLabel(it) },
                    onSelect = {
                        XLog.tag(TAG).i("Connection strategy selected [strategy=%s]", it)
                        viewModel.setConnectionStrategy(it)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // Bypass method card
            BypassCard {
                CardHeader(stringResource(R.string.bypass_methods_title))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 12.dp)
                        .selectableGroup(),
                ) {
                    BypassMethodRadioRow(
                        label = stringResource(R.string.tls_handshake_obfuscation_radio_button_label),
                        selected = bypassMethod == BypassCensorshipMethod.TLS_OBFUSCATION,
                        onSelect = {
                            XLog.tag(TAG).i("Bypass method selected [method=TLS_OBFUSCATION]")
                            viewModel.setBypassMethod(BypassCensorshipMethod.TLS_OBFUSCATION)
                        },
                    )
                    BypassMethodRadioRow(
                        label = stringResource(R.string.sni_reality_radio_button_label),
                        selected = bypassMethod == BypassCensorshipMethod.SNI_REALITY,
                        onSelect = {
                            XLog.tag(TAG).i("Bypass method selected [method=SNI_REALITY]")
                            viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY)
                        },
                    )
                }
                if (showSniSection) {
                    LegacySpinner(
                        items = SniSpoofingMode.entries,
                        selected = sniSpoofingMode,
                        label = { sniSpoofingModeLabel(it) },
                        onSelect = { viewModel.setSniSpoofingMode(it) },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            if (showSniSection) {
                // SNI settings card
                BypassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditSniDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_baseline_your_servers_24),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(32.dp),
                        )
                        Text(text = stringResource(R.string.your_current_sni), color = White)
                        Text(
                            text = currentSni,
                            color = White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = stringResource(R.string.sni_text_description),
                        color = Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 5.dp),
                    )
                }

                // SNI autoscan card
                BypassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, top = 10.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_sql_server_24),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(28.dp),
                        )
                        Text(text = stringResource(R.string.auto_sni_label), color = White)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.loaded_sni_label),
                            color = White,
                            modifier = Modifier.padding(end = 5.dp),
                        )
                        Text(text = sniCount.toString(), color = White)
                    }

                    if (serviceState == SniCheckerServiceState.ACTIVE) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            ) {
                                Text(text = stringResource(R.string.server_label), color = White)
                                Text(
                                    text = selectedCheckingServer.serverInfo,
                                    color = White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            ) {
                                Text(text = stringResource(R.string.checking_current_sni), color = White)
                                Text(
                                    text = currentCheckingSni,
                                    color = White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LinearProgressIndicator(
                                    progress = {
                                        if (currentProgress.second > 0) {
                                            currentProgress.first.toFloat() / currentProgress.second.toFloat()
                                        } else {
                                            0f
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                )
                                Text(
                                    text = "${currentProgress.first}/${currentProgress.second}",
                                    color = White,
                                )
                            }
                        }
                    }

                    val toggleChecked = serviceState == SniCheckerServiceState.ACTIVE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .legacyDrawableBackground(R.drawable.round_back_secondary_cancel_100)
                            .then(
                                if (sniCount > 0 || toggleChecked) {
                                    Modifier.clickable { onAutoSelectClicked() }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(8.dp),
                    ) {
                        Text(
                            text = if (toggleChecked) {
                                stringResource(R.string.stop_sni_checking_button_label)
                            } else {
                                stringResource(R.string.auto_select_sni)
                            },
                            color = if (toggleChecked) White else Primary,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
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

    autoSelectServers?.let { servers ->
        AutoSelectSniDialog(
            servers = servers,
            onCancel = { autoSelectServers = null },
            onStart = { server, resetChecked ->
                XLog.tag(TAG).i(
                    "Starting SNI auto-select [server=%s, resetChecked=%b]",
                    server.serverInfo,
                    resetChecked,
                )
                SniCheckerService.startChecking(context, server, resetChecked, bypassMethod)
                autoSelectServers = null
            },
        )
    }

    if (showEditSniDialog) {
        EditSniDialog(
            initialSni = viewModel.currentSni.orEmpty(),
            suggestions = remember { loadSniSuggestions(context) },
            onSave = {
                XLog.tag(TAG).i("SNI edit saved [sni=%s]", it)
                viewModel.validateAndSetSni(it)
            },
            onResetDefault = {
                XLog.tag(TAG).i("SNI reset to default")
                viewModel.resetToDefault()
            },
            onDismiss = { showEditSniDialog = false },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

@Composable
private fun BypassCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .padding(10.dp),
        content = { content() },
    )
}

@Composable
private fun CardHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_logo_24),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 4.dp, end = 12.dp)
                .size(32.dp),
        )
        Text(text = title, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BypassMethodRadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(top = 1.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
    }
}

@Composable
private fun <T> LegacySpinner(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .legacyDrawableBackground(R.drawable.round_back_spinner_down)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label(selected),
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(label(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AutoSelectSniDialog(
    servers: List<ServerEntity>,
    onCancel: () -> Unit,
    onStart: (ServerEntity, Boolean) -> Unit,
) {
    var selected by remember(servers) { mutableStateOf(servers.firstOrNull() ?: ServerEntity.AUTO) }
    var resetChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column {
                Text(stringResource(R.string.select_server_for_autoselect), modifier = Modifier.padding(bottom = 16.dp))
                ServerDropdown(servers = servers, selected = selected, onSelect = { selected = it })
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { resetChecked = !resetChecked },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = resetChecked, onCheckedChange = { resetChecked = it })
                    Text(stringResource(R.string.reset_checked_previously_checkbox_label))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(selected, resetChecked) }) {
                Text(stringResource(R.string.start_sni_checking_button_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun EditSniDialog(
    initialSni: String,
    suggestions: List<String>,
    onSave: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialSni) }
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(text, suggestions) {
        if (text.isEmpty()) emptyList() else suggestions.filter { it.contains(text, ignoreCase = true) }.take(20)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        expanded = it.isNotEmpty()
                    },
                    label = { Text(stringResource(R.string.sni_text_view_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = expanded && filteredSuggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                text = suggestion
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(text)
                onDismiss()
            }) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onResetDefault()
                onDismiss()
            }) {
                Text(stringResource(R.string.reset_default_button))
            }
        },
    )
}

private fun loadSniSuggestions(context: Context): List<String> {
    val result = mutableListOf<String>()
    if (Locale.getDefault().language == "ru") {
        result.addAll(readSniRawFile(context, R.raw.russia))
    }
    result.addAll(readSniRawFile(context, R.raw.global))
    return result
}

private fun readSniRawFile(context: Context, rawResId: Int): List<String> {
    val list = mutableListOf<String>()
    try {
        context.resources.openRawResource(rawResId).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        list.add(trimmed)
                    }
                }
            }
        }
    } catch (e: Exception) {
        XLog.tag(TAG).e("Failed to read SNI suggestions file: %s", e.message)
    }
    return list
}

@Composable
private fun connectionStrategyLabel(strategy: ConnectionStrategy): String = when (strategy) {
    ConnectionStrategy.DUAL_TUNNEL -> stringResource(R.string.connection_strategy_dual)
    ConnectionStrategy.ROLLING_TUNNEL -> stringResource(R.string.connection_strategy_rolling)
    ConnectionStrategy.TRIPLE_TUNNEL -> stringResource(R.string.connection_strategy_triple)
}

@Composable
private fun sniSpoofingModeLabel(mode: SniSpoofingMode): String = when (mode) {
    SniSpoofingMode.SNI -> stringResource(R.string.sni)
    SniSpoofingMode.SNI_REALITY_CHROME_149 -> stringResource(R.string.sni_reality_radio_button_label_chrome_149)
    SniSpoofingMode.SNI_REALITY_CHROME_148 -> stringResource(R.string.sni_reality_radio_button_label_chrome_148)
    SniSpoofingMode.SNI_REALITY_CHROME_147 -> stringResource(R.string.sni_reality_radio_button_label_chrome_147)
    SniSpoofingMode.SNI_REALITY_CHROME_146 -> stringResource(R.string.sni_reality_radio_button_label_chrome_146)
    SniSpoofingMode.SNI_REALITY_CHROME_145 -> stringResource(R.string.sni_reality_radio_button_label_chrome_145)
    SniSpoofingMode.SNI_REALITY_FIREFOX_151 -> stringResource(R.string.sni_reality_radio_button_label_firefox_151)
    SniSpoofingMode.SNI_REALITY_FIREFOX_150 -> stringResource(R.string.sni_reality_radio_button_label_firefox_150)
    SniSpoofingMode.SNI_REALITY_FIREFOX_149 -> stringResource(R.string.sni_reality_radio_button_label_firefox_149)
    SniSpoofingMode.SNI_REALITY_YANDEX_26_4 -> stringResource(R.string.sni_reality_radio_button_label_yandex_26_4)
    SniSpoofingMode.SNI_REALITY_YANDEX_26_3 -> stringResource(R.string.sni_reality_radio_button_label_yandex_26_3)
    SniSpoofingMode.SNI_REALITY_YANDEX_25 -> stringResource(R.string.sni_reality_radio_button_label_yandex_25)
    SniSpoofingMode.SNI_REALITY_YANDEX_24 -> stringResource(R.string.sni_reality_radio_button_label_yandex_24)
    SniSpoofingMode.SNI_REALITY_SAFARI_26_5 -> stringResource(R.string.sni_reality_radio_button_label_safari_26_5)
    SniSpoofingMode.SNI_REALITY_SAFARI_26_4 -> stringResource(R.string.sni_reality_radio_button_label_safari_26_4)
}
