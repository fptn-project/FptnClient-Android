package org.fptn.vpn.ui.perappvpn

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.enums.PerAppVpnMode
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LinkifiedText
import org.fptn.vpn.ui.common.MultilineTextInputDialog
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.Gray
import org.fptn.vpn.ui.theme.TealAccent
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.views.perappvpn.AppInfo
import org.fptn.vpn.views.perappvpn.PerAppVpnModeViewModel

private const val TAG = "PerAppVpnModeScreen"

/**
 * Compose port of the legacy `PerAppVpnModeActivity` / `settings_per_app_vpnmode_layout.xml`.
 * Reuses [PerAppVpnModeViewModel] unchanged; the search/sort/toggle logic that used to live in
 * `AppInfoListAdapter` is reproduced here since a RecyclerView adapter has no Compose analogue.
 */
@Composable
fun PerAppVpnModeScreen(
    viewModel: PerAppVpnModeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val mode by viewModel.perAppVpnModeMutableLiveData.observeAsState(PerAppVpnMode.OFF)
    val apps by viewModel.appListMutableLiveData.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(viewModel.isShowSystemApps()) }
    var splitTunnelEnabled by remember { mutableStateOf(SharedPrefUtils.getSplitTunnelDomainsEnabled(context)) }
    var showSplitTunnelDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    // Bumped after mutating an AppInfo in place (it's a plain mutable Java object shared with
    // the ViewModel, not Compose state), to force the derived list below to recompute.
    var toggleVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context.packageManager)
    }

    val displayApps = remember(apps, mode, searchQuery, toggleVersion) {
        apps
            .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
            .sortedWith(
                compareByDescending<AppInfo> { isActive(it, mode) }
                    .thenBy { it.label.lowercase() },
            )
    }

    fun onToggle(app: AppInfo) {
        when (mode) {
            PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed = !app.isAllowed
            PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed = !app.isDisallowed
            else -> Unit
        }
        toggleVersion++
        viewModel.saveSelectedApps()
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
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_per_app_vpn_mode),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .size(80.dp),
            )
            Text(
                text = stringResource(R.string.per_app_vpn_settings_title),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            HtmlLinkText(
                html = stringResource(R.string.settings_per_app_russian_notice),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = Gray,
                fontSize = 12.sp,
            )

            // Split tunneling card
            SettingsCard {
                Text(
                    text = stringResource(R.string.split_tunnel_title),
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinkifiedText(
                        text = stringResource(R.string.split_tunnel_enable),
                        linkText = stringResource(R.string.split_tunnel_enable_link),
                        onLinkClick = { showSplitTunnelDialog = true },
                        color = White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = splitTunnelEnabled,
                        onCheckedChange = {
                            splitTunnelEnabled = it
                            SharedPrefUtils.saveSplitTunnelDomainsEnabled(context, it)
                        },
                    )
                }
            }

            // Per-app VPN method card
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_24),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 12.dp)
                            .size(32.dp),
                    )
                    Text(
                        text = stringResource(R.string.per_app_vpn_mode_title),
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 12.dp, top = 8.dp)
                        .selectableGroup(),
                ) {
                    PerAppVpnModeRadioRow(
                        label = stringResource(R.string.only_allowed_apps_mode_radio_button_label),
                        info = stringResource(R.string.only_allowed_radio_button_label_additional_info),
                        selected = mode == PerAppVpnMode.ONLY_ALLOWED,
                        onSelect = {
                            XLog.tag(TAG).i("Per-app VPN mode changed [mode=ONLY_ALLOWED]")
                            viewModel.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED)
                        },
                    )
                    PerAppVpnModeRadioRow(
                        label = stringResource(R.string.disallowed_apps_radio_button_label),
                        info = stringResource(R.string.disallowed_apps_radio_button_label_additional_info),
                        selected = mode == PerAppVpnMode.EXCEPT_DISALLOWED,
                        onSelect = {
                            XLog.tag(TAG).i("Per-app VPN mode changed [mode=EXCEPT_DISALLOWED]")
                            viewModel.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED)
                        },
                    )
                }
            }

            // Select apps list card (legacy quirk: this is always shown, never hidden, in any mode)
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = White, fontSize = 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(stringResource(R.string.search_apps_hint), color = Gray, fontSize = 14.sp)
                            }
                            inner()
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.show_system_apps_label),
                        color = Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = {
                            showSystemApps = it
                            viewModel.setShowSystemApps(it)
                        },
                    )
                }

                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                        items(displayApps, key = { it.packageName }) { app ->
                            AppRow(app = app, mode = mode, onToggle = { onToggle(app) })
                        }
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

    if (showSplitTunnelDialog) {
        MultilineTextInputDialog(
            title = stringResource(R.string.split_tunnel_title),
            hint = stringResource(R.string.domain_blacklist_hint),
            initialText = remember { SharedPrefUtils.getSplitTunnelDomains(context) },
            onSave = { SharedPrefUtils.saveSplitTunnelDomains(context, it) },
            onDismiss = { showSplitTunnelDialog = false },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

private fun isActive(app: AppInfo, mode: PerAppVpnMode): Boolean = when {
    app.isForcedExcluded -> true
    mode == PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed
    mode == PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed
    else -> false
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
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
private fun PerAppVpnModeRadioRow(label: String, info: String, selected: Boolean, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(top = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
        }
        Text(
            text = info,
            color = Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 30.dp),
        )
    }
}

@Composable
private fun AppRow(app: AppInfo, mode: PerAppVpnMode, onToggle: () -> Unit) {
    val forcedExcluded = app.isForcedExcluded
    val checked = when {
        forcedExcluded -> mode == PerAppVpnMode.EXCEPT_DISALLOWED
        mode == PerAppVpnMode.ONLY_ALLOWED -> app.isAllowed
        mode == PerAppVpnMode.EXCEPT_DISALLOWED -> app.isDisallowed
        else -> false
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = remember(app.packageName) { app.icon?.toBitmapCompat() }
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = app.label,
            color = if (forcedExcluded) TealAccent else White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = !forcedExcluded,
        )
    }
}

private fun Drawable.toBitmapCompat(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
