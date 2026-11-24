package org.fptn.vpn.ui.perappvpn

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
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
import org.fptn.vpn.ui.common.LinkifiedText
import org.fptn.vpn.ui.common.MultilineTextInputDialog
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.theme.cardContainerColor
import org.fptn.vpn.utils.SharedPrefUtils

private const val TAG = "PerAppVpnModeScreen"

@Composable
fun PerAppVpnModeScreen(
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: PerAppVpnModeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val mode by viewModel.perAppVpnModeMutableLiveData.observeAsState(PerAppVpnMode.OFF)
    val apps by viewModel.appListMutableLiveData.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(viewModel.showSystemApps) }
    var splitTunnelEnabled by remember { mutableStateOf(SharedPrefUtils.getSplitTunnelDomainsEnabled(context)) }
    var showSplitTunnelDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Раздельное туннелирование",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector & Domain blacklist card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Split Tunnel domains toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinkifiedText(
                            text = stringResource(R.string.split_tunnel_enable),
                            linkText = stringResource(R.string.split_tunnel_enable_link),
                            onLinkClick = { showSplitTunnelDialog = true },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Radio buttons
                    Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                        PerAppVpnModeRadioRow(
                            label = stringResource(R.string.only_allowed_apps_mode_radio_button_label),
                            selected = mode == PerAppVpnMode.ONLY_ALLOWED,
                            onSelect = {
                                XLog.tag(TAG).i("Per-app VPN mode changed [mode=ONLY_ALLOWED]")
                                viewModel.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED)
                            },
                        )
                        PerAppVpnModeRadioRow(
                            label = stringResource(R.string.disallowed_apps_radio_button_label),
                            selected = mode == PerAppVpnMode.EXCEPT_DISALLOWED,
                            onSelect = {
                                XLog.tag(TAG).i("Per-app VPN mode changed [mode=EXCEPT_DISALLOWED]")
                                viewModel.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & System Apps Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(stringResource(R.string.search_apps_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                }
                                inner()
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.show_system_apps_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Apps List taking full remaining height
            if (apps.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(displayApps, key = { it.packageName }) { app ->
                        AppRow(app = app, mode = mode, onToggle = { onToggle(app) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavBar(
                isHomeScreen = false,
                isSettingsScreen = false,
                onNavigateHome = onNavigateHome,
                onNavigateSettings = onNavigateSettings,
                onShare = { showShareDialog = true },
            )
        }
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
private fun PerAppVpnModeRadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.cardContainerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = remember(app.packageName) { app.icon?.toBitmapCompat() }
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
            Text(
                text = app.label,
                color = if (forcedExcluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
