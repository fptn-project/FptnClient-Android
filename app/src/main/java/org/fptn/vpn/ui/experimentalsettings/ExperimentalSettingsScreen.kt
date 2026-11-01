package org.fptn.vpn.ui.experimentalsettings

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.core.common.Constants
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.services.vpn.FptnService
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.LinkifiedText
import org.fptn.vpn.ui.common.MultilineTextInputDialog
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.findActivity
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.utils.SharedPrefUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.alpha
import org.fptn.vpn.ui.theme.FontScaleState
private const val TAG = "ExperimentalSettingsScreen"
private val ATTEMPTS_COUNT_VALUES = intArrayOf(5, 15, 35, Int.MAX_VALUE)
private val FALLBACK_THRESHOLD_VALUES = intArrayOf(3, 6, 10, 15)
private val FONT_SCALE_LABELS = arrayOf("0.85×", "1.0×", "1.1×", "1.2×")

/**
 * Compose port of the legacy `ExperimentalSettingsActivity` / `experimental_settings_layout.xml`.
 * No ViewModel existed here either — every switch/seekbar reads its initial value from
 * `SharedPrefUtils` once and writes back on change, reproduced the same way with local
 * `remember` state instead of View fields.
 */
@Composable
fun ExperimentalSettingsScreen(
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTv = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_TELEVISION != 0
    var showShareDialog by remember { mutableStateOf(false) }
    var showDomainBlacklistDialog by remember { mutableStateOf(false) }

    var killSwitchEnabled by remember { mutableStateOf(SharedPrefUtils.getKillSwitchEnabled(context)) }
    var killSwitchOnNetworkChange by remember { mutableStateOf(SharedPrefUtils.getKillSwitchOnNetworkChangeEnabled(context)) }
    var customDnsEnabled by remember { mutableStateOf(SharedPrefUtils.getCustomDnsEnabled(context)) }
    var customDnsInput by remember { mutableStateOf(SharedPrefUtils.getCustomDnsIpv4(context)) }
    val customDnsValid = customDnsInput.isEmpty() || isValidDnsAddress(customDnsInput)
    var showSpeedInNotification by remember { mutableStateOf(SharedPrefUtils.getShowSpeedInNotification(context)) }
    var showTrafficInNotification by remember { mutableStateOf(SharedPrefUtils.getShowTrafficInNotification(context)) }
    var showTrafficChart by remember { mutableStateOf(SharedPrefUtils.getShowTrafficChart(context)) }
    var allowLandscape by remember { mutableStateOf(SharedPrefUtils.getAllowLandscape(context)) }
    var adBlockEnabled by remember { mutableStateOf(SharedPrefUtils.getAdBlockEnabled(context)) }
    var domainBlacklistEnabled by remember { mutableStateOf(SharedPrefUtils.getDomainBlacklistEnabled(context)) }
    var domainBlacklistAdvancedEnabled by remember { mutableStateOf(SharedPrefUtils.getDomainBlacklistAdvancedModeEnabled(context)) }
    var reconnectOnChangeNetworkType by remember { mutableStateOf(SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(context)) }
    var reconnectOnChangeIp by remember { mutableStateOf(SharedPrefUtils.getReconnectOnChangeIPEnabled(context)) }
    var attemptsCountProgress by remember { mutableFloatStateOf(initialAttemptsCountProgress(context)) }
    var delayBetweenProgress by remember { mutableFloatStateOf((SharedPrefUtils.getDelayBetweenReconnect(context) - 1).toFloat()) }
    var resetServerAfterDisconnect by remember { mutableStateOf(SharedPrefUtils.getResetSelectedServerEnabled(context)) }
    var resetServerAfterDisconnectOnException by remember { mutableStateOf(SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(context)) }
    var connectFailedHelpEnabled by remember { mutableStateOf(SharedPrefUtils.getConnectFailedHelpEnabled(context)) }
    var autoFallbackEnabled by remember { mutableStateOf(SharedPrefUtils.getAutoFallbackEnabled(context)) }
    var fallbackThresholdProgress by remember { mutableFloatStateOf(initialFallbackThresholdProgress(context)) }
    var tileButtonEnabled by remember { mutableStateOf(true) }
    var tileButtonUsedUp by remember { mutableStateOf(false) }

    var useSystemFontSize by remember { mutableStateOf(FontScaleState.useSystem) }
    var fontScaleProgress by remember { mutableFloatStateOf(FontScaleState.index.toFloat()) }
    var autoStartOnBoot by remember { mutableStateOf(SharedPrefUtils.getAutoStartOnBoot(context)) }

    fun resetToDefault() {
        killSwitchEnabled = false
        killSwitchOnNetworkChange = false
        customDnsEnabled = false
        customDnsInput = ""
        showSpeedInNotification = false
        showTrafficInNotification = false
        showTrafficChart = true
        adBlockEnabled = true
        domainBlacklistEnabled = true
        domainBlacklistAdvancedEnabled = false
        reconnectOnChangeNetworkType = true
        reconnectOnChangeIp = true
        attemptsCountProgress = 2f
        delayBetweenProgress = 0f
        resetServerAfterDisconnect = true
        resetServerAfterDisconnectOnException = false
        connectFailedHelpEnabled = true
        autoFallbackEnabled = true
        fallbackThresholdProgress = 3f
        useSystemFontSize = true
        fontScaleProgress = Constants.FONT_SCALE_DEFAULT_INDEX.toFloat()
        // Synchronize global state — instant re-render
        FontScaleState.setUseSystem(context, true)
        FontScaleState.setIndex(context, Constants.FONT_SCALE_DEFAULT_INDEX)

        SharedPrefUtils.saveKillSwitchEnabled(context, false)
        SharedPrefUtils.saveKillSwitchOnNetworkChangeEnabled(context, false)
        SharedPrefUtils.saveCustomDnsEnabled(context, false)
        SharedPrefUtils.saveCustomDnsIpv4(context, "")
        SharedPrefUtils.saveShowSpeedInNotification(context, false)
        SharedPrefUtils.saveShowTrafficInNotification(context, false)
        SharedPrefUtils.saveShowTrafficChart(context, true)
        SharedPrefUtils.saveAdBlockEnabled(context, true)
        SharedPrefUtils.saveDomainBlacklistEnabled(context, true)
        SharedPrefUtils.saveDomainBlacklistDomains(context, Constants.DOMAIN_BLACKLIST_DEFAULT)
        SharedPrefUtils.saveDomainBlacklistAdvancedModeEnabled(context, false)
        SharedPrefUtils.saveSplitTunnelDomainsEnabled(context, true)
        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(context, true)
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(context, true)
        SharedPrefUtils.saveReconnectAttemptsCount(context, ATTEMPTS_COUNT_VALUES[2])
        SharedPrefUtils.saveDelayBetweenReconnect(context, 1)
        SharedPrefUtils.saveResetSelectedServerEnabled(context, true)
        SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(context, false)
        SharedPrefUtils.saveConnectFailedHelpEnabled(context, true)
        SharedPrefUtils.saveAutoFallbackEnabled(context, true)
        SharedPrefUtils.saveAutoFallbackThreshold(context, FALLBACK_THRESHOLD_VALUES[3])

        autoStartOnBoot = false
        SharedPrefUtils.saveAutoStartOnBoot(context, false)

        XLog.tag(TAG).i("Experimental settings reset to default")
        Toast.makeText(context, R.string.reset_to_default_success, Toast.LENGTH_SHORT).show()
    }

    fun requestQuickSettingsTile() {
        val statusBarManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(StatusBarManager::class.java) ?: return
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
        tileButtonEnabled = false
        try {
            val componentName = ComponentName(context, FptnTileService::class.java)
            val label = context.getString(R.string.app_name)
            val icon = Icon.createWithResource(context, R.drawable.ic_logo)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                statusBarManager.requestAddTileService(
                    componentName,
                    label,
                    icon,
                    context.mainExecutor,
                ) { resultCode ->
                    when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                            XLog.tag(TAG).i("Quick settings tile already present")
                            Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
                            tileButtonUsedUp = true
                        }
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                            XLog.tag(TAG).i("Quick settings tile added successfully")
                            Toast.makeText(context, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show()
                            tileButtonUsedUp = true
                        }
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> {
                            XLog.tag(TAG).w("Quick settings tile request was declined")
                            tileButtonEnabled = true
                        }
                        else -> {
                            XLog.tag(TAG).w("Quick settings tile request returned unexpected result [code=%d]", resultCode)
                            tileButtonEnabled = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            XLog.tag(TAG).e("Failed to request quick settings tile addition: %s", e.message)
            Toast.makeText(context, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show()
            tileButtonEnabled = true
        }
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
                painter = painterResource(R.drawable.ic_logo_24),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .size(80.dp),
            )
            Text(
                text = stringResource(R.string.experimental_features_label),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
            )

            if (!isTv) {
                ExperimentalCard {
                    CardTitle(stringResource(R.string.orientation_settings_title))
                    SwitchRow(
                        text = stringResource(R.string.allow_landscape_label),
                        checked = allowLandscape,
                        onCheckedChange = {
                            allowLandscape = it
                            SharedPrefUtils.saveAllowLandscape(context, it)
                            context.findActivity()?.requestedOrientation = if (it) {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                    )
                }
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.ad_block_title))
                CardDescription(stringResource(R.string.ad_block_description))
                SwitchRow(
                    text = stringResource(R.string.ad_block_enable),
                    checked = adBlockEnabled,
                    onCheckedChange = {
                        adBlockEnabled = it
                        SharedPrefUtils.saveAdBlockEnabled(context, it)
                    },
                )
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.domain_blacklist_title))
                CardDescription(stringResource(R.string.domain_blacklist_description))

                Text(
                    text = stringResource(R.string.domain_blacklist_list_link),
                    color = White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        .clickable { showDomainBlacklistDialog = true }
                )

                SwitchRow(
                    text = stringResource(R.string.domain_blacklist_enable),
                    checked = domainBlacklistEnabled,
                    onCheckedChange = {
                        domainBlacklistEnabled = it
                        SharedPrefUtils.saveDomainBlacklistEnabled(context, it)
                        
                        if (!it) {
                            domainBlacklistAdvancedEnabled = false
                            SharedPrefUtils.saveDomainBlacklistAdvancedModeEnabled(context, false)
                        }
                    },
                )

                SwitchRow(
                    text = stringResource(R.string.domain_blacklist_advanced_enable),
                    checked = domainBlacklistAdvancedEnabled,
                    enabled = domainBlacklistEnabled,
                    onCheckedChange = {
                        domainBlacklistAdvancedEnabled = it
                        SharedPrefUtils.saveDomainBlacklistAdvancedModeEnabled(context, it)
                    },
                )
                
                CardDescription(stringResource(R.string.domain_blacklist_advanced_description))
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.custom_dns_title))
                CardDescription(stringResource(R.string.custom_dns_description))
                SwitchRow(
                    text = stringResource(R.string.custom_dns_enable),
                    checked = customDnsEnabled,
                    onCheckedChange = {
                        customDnsEnabled = it
                        SharedPrefUtils.saveCustomDnsEnabled(context, it)
                    },
                )
                if (customDnsEnabled) {
                    TextField(
                        value = customDnsInput,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            customDnsInput = filtered
                            if (isValidDnsAddress(filtered)) {
                                SharedPrefUtils.saveCustomDnsIpv4(context, filtered)
                            }
                        },
                        placeholder = { Text(stringResource(R.string.custom_dns_hint)) },
                        isError = customDnsInput.isNotEmpty() && !customDnsValid,
                        supportingText = {
                            if (customDnsInput.isNotEmpty() && !customDnsValid) {
                                Text(stringResource(R.string.custom_dns_invalid))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.speed_notification_title))
                SwitchRow(
                    text = stringResource(R.string.speed_notification_enable),
                    checked = showSpeedInNotification,
                    onCheckedChange = {
                        showSpeedInNotification = it
                        SharedPrefUtils.saveShowSpeedInNotification(context, it)
                    },
                )
                SwitchRow(
                    text = stringResource(R.string.traffic_notification_enable),
                    checked = showTrafficInNotification,
                    onCheckedChange = {
                        showTrafficInNotification = it
                        SharedPrefUtils.saveShowTrafficInNotification(context, it)
                    },
                )
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.home_screen_settings_title))
                SwitchRow(
                    text = stringResource(R.string.show_traffic_chart_label),
                    checked = showTrafficChart,
                    onCheckedChange = {
                        showTrafficChart = it
                        SharedPrefUtils.saveShowTrafficChart(context, it)
                    },
                )
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.connect_failed_help_settings_title))
                CardDescription(stringResource(R.string.connect_failed_help_settings_description))
                SwitchRow(
                    text = stringResource(R.string.connect_failed_help_enable),
                    checked = connectFailedHelpEnabled,
                    onCheckedChange = {
                        connectFailedHelpEnabled = it
                        SharedPrefUtils.saveConnectFailedHelpEnabled(context, it)
                    },
                )
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.auto_fallback_title))
                SwitchRow(
                    text = stringResource(R.string.auto_fallback_enable),
                    checked = autoFallbackEnabled,
                    onCheckedChange = {
                        autoFallbackEnabled = it
                        SharedPrefUtils.saveAutoFallbackEnabled(context, it)
                    },
                )
                if (autoFallbackEnabled) {
                    DiscreteSeekBar(
                        label = stringResource(R.string.auto_fallback_threshold_label),
                        valueText = String.format(stringResource(R.string.auto_fallback_threshold_value), FALLBACK_THRESHOLD_VALUES[fallbackThresholdProgress.toInt()]),
                        progress = fallbackThresholdProgress,
                        onProgressChange = {
                            fallbackThresholdProgress = it
                            SharedPrefUtils.saveAutoFallbackThreshold(context, FALLBACK_THRESHOLD_VALUES[it.toInt()])
                        },
                    )
                }
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.reconnection_on_failure_attempts))
                CardDescription(stringResource(R.string.reconnect_attempts_description))
                val attemptsValue = ATTEMPTS_COUNT_VALUES[attemptsCountProgress.toInt()]
                DiscreteSeekBar(
                    label = stringResource(R.string.attempts_count),
                    valueText = if (attemptsValue == Int.MAX_VALUE) {
                        "∞"
                    } else {
                        String.format(stringResource(R.string.reconnect_attempts_text), attemptsValue)
                    },
                    progress = attemptsCountProgress,
                    onProgressChange = {
                        attemptsCountProgress = it
                        val value = ATTEMPTS_COUNT_VALUES[it.toInt()]
                        SharedPrefUtils.saveReconnectAttemptsCount(context, value)
                    },
                )
                DiscreteSeekBar(
                    label = stringResource(R.string.delay_between_attempts),
                    valueText = String.format(stringResource(R.string.delay_between_attempts_seconds), delayBetweenProgress.toInt() + 1),
                    progress = delayBetweenProgress,
                    onProgressChange = {
                        delayBetweenProgress = it
                        SharedPrefUtils.saveDelayBetweenReconnect(context, it.toInt() + 1)
                    },
                )

                // Warning if an unlimited number of attempts is selected
                if (attemptsValue == Int.MAX_VALUE) {
                    Text(
                        text = stringResource(R.string.kill_switch_infinite_warning),
                        color = White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            ExperimentalCard {
                CardTitle(stringResource(R.string.reconnect_on_change))
                SwitchRow(
                    text = stringResource(R.string.reconnect_on_change_network_type),
                    checked = reconnectOnChangeNetworkType,
                    onCheckedChange = {
                        reconnectOnChangeNetworkType = it
                        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(context, it)
                    },
                )
                SwitchRow(
                    text = stringResource(R.string.reconnect_on_change_ip_address),
                    checked = reconnectOnChangeIp,
                    onCheckedChange = {
                        reconnectOnChangeIp = it
                        SharedPrefUtils.saveReconnectOnChangeIPEnabled(context, it)
                    },
                )
            }

            if (!isTv) {
                ExperimentalCard(paddingBottom = 8.dp) {
                    CardTitle(stringResource(R.string.font_scale_title))
                    CardDescription(stringResource(R.string.font_scale_description))

                    SwitchRow(
                        text = stringResource(R.string.font_scale_use_system),
                        checked = useSystemFontSize,
                        onCheckedChange = {
                            useSystemFontSize = it
                            // Updating global state → instant re-rendering of the entire UI
                            FontScaleState.setUseSystem(context, it)
                        },
                    )

                    DiscreteSeekBar(
                        label = stringResource(R.string.font_scale_custom_label),
                        valueText = FONT_SCALE_LABELS[fontScaleProgress.toInt()],
                        progress = fontScaleProgress,
                        enabled = !useSystemFontSize,
                        onProgressChange = {
                            fontScaleProgress = it
                            // Updating global state → instant re-rendering of the entire UI
                            FontScaleState.setIndex(context, it.toInt())
                            XLog.tag(TAG).i("Font scale changed [index=%d, value=%.2f]",
                                it.toInt(), Constants.FONT_SCALE_VALUES[it.toInt()])
                        },
                    )
                }
            }

            ExperimentalCard(paddingBottom = 8.dp) {
                CardTitle(stringResource(R.string.kill_switch_title))
                CardDescription(stringResource(R.string.kill_switch_description))
                SwitchRow(
                    text = stringResource(R.string.kill_switch_enable),
                    checked = killSwitchEnabled,
                    onCheckedChange = {
                        killSwitchEnabled = it
                        SharedPrefUtils.saveKillSwitchEnabled(context, it)
                        if (!it && FptnTileService.getServiceStateMutableLiveData().value == ConnectionState.BLOCKED) {
                            FptnService.startToDisconnect(context)
                        }
                    },
                )

                SwitchRow(
                    text = stringResource(R.string.kill_switch_network_change_enable),
                    checked = killSwitchOnNetworkChange,
                    enabled = killSwitchEnabled,
                    onCheckedChange = {
                        killSwitchOnNetworkChange = it
                        SharedPrefUtils.saveKillSwitchOnNetworkChangeEnabled(context, it)
                    },
                )
                Text(
                    text = stringResource(R.string.kill_switch_network_change_description),
                    color = White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Text(
                    text = stringResource(R.string.kill_switch_system_hint),
                    color = White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = stringResource(R.string.kill_switch_system_button),
                    color = White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                        .padding(vertical = 3.dp),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isTv) {
                ExperimentalCard(paddingBottom = 8.dp) {
                    CardTitle(stringResource(R.string.quick_settings_tile_title))
                    Text(
                        text = stringResource(R.string.quick_settings_tile_description),
                        color = White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Text(
                        text = stringResource(R.string.quick_settings_tile_button),
                        color = White.copy(alpha = if (tileButtonUsedUp) 0.5f * 0.7f else 0.7f),
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = tileButtonEnabled) { requestQuickSettingsTile() }
                            .padding(vertical = 3.dp),
                    )
                }
            }

            ExperimentalCard(paddingBottom = 8.dp) {
                CardTitle(stringResource(R.string.settings_reset_server_title))
                SwitchRow(
                    text = stringResource(R.string.settings_reset_server_on_disconnect),
                    checked = resetServerAfterDisconnect,
                    onCheckedChange = {
                        resetServerAfterDisconnect = it
                        SharedPrefUtils.saveResetSelectedServerEnabled(context, it)
                    },
                )
                if (!resetServerAfterDisconnect) {
                    SwitchRow(
                        text = stringResource(R.string.settings_reset_server_on_exception),
                        checked = resetServerAfterDisconnectOnException,
                        onCheckedChange = {
                            resetServerAfterDisconnectOnException = it
                            SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(context, it)
                        },
                    )
                }
            }

            ExperimentalCard(paddingBottom = 8.dp) {
                CardTitle(stringResource(R.string.auto_start_on_boot_title))
                CardDescription(stringResource(R.string.auto_start_on_boot_description))
                SwitchRow(
                    text = stringResource(R.string.auto_start_on_boot_enable),
                    checked = autoStartOnBoot,
                    onCheckedChange = {
                        autoStartOnBoot = it
                        SharedPrefUtils.saveAutoStartOnBoot(context, it)
                    },
                )
                // Hint with bold OEM names — single line spacing (lineHeight = 17.sp)
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.auto_start_on_boot_hint_before))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.auto_start_on_boot_hint_oems))
                        }
                        append(stringResource(R.string.auto_start_on_boot_hint_after))
                    },
                    color = White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Text(
                text = stringResource(R.string.reset_to_default_button),
                color = White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clickable { resetToDefault() }
                    .padding(vertical = 3.dp),
            )
        }

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = onNavigateHome,
            onNavigateSettings = onNavigateSettings,
            onShare = { showShareDialog = true },
        )
    }

    if (showDomainBlacklistDialog) {
        MultilineTextInputDialog(
            title = stringResource(R.string.domain_blacklist_title),
            hint = stringResource(R.string.domain_blacklist_hint),
            initialText = remember { SharedPrefUtils.getDomainBlacklistDomains(context) },
            onSave = { SharedPrefUtils.saveDomainBlacklistDomains(context, it) },
            onDismiss = { showDomainBlacklistDialog = false },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

@Composable
private fun ExperimentalCard(paddingBottom: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = paddingBottom),
        content = content,
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        color = White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 22.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun CardDescription(text: String) {
    Text(
        text = text,
        color = White.copy(alpha = 0.7f),
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text, 
            color = if (enabled) White else White.copy(alpha = 0.5f),
            fontSize = 14.sp, 
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked, 
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DiscreteSeekBar(
    label: String,
    valueText: String,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = label,
                color = if (enabled) White else White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
            Text(
                text = valueText,
                color = if (enabled) White else White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val trackColor = Color(0xFF959292)
        val activeColor = Color(0xFF03DAC5)
        val tickColor = Color(0xFF03DAC5).copy(alpha = 0.7f)
        val thumbColor = Color(0xFFFFFFFF)

        val maxStep = 3
        val tickFractions = (1 until maxStep).map { it.toFloat() / maxStep }

        val currentProgress by rememberUpdatedState(progress)
        val currentOnProgressChange by rememberUpdatedState(onProgressChange)
        val currentEnabled by rememberUpdatedState(enabled)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .focusable(enabled)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && currentEnabled) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                val newValue = (currentProgress - 1).coerceIn(0f, maxStep.toFloat())
                                if (newValue != currentProgress) {
                                    currentOnProgressChange(newValue)
                                }
                                true
                            }
                            Key.DirectionRight -> {
                                val newValue = (currentProgress + 1).coerceIn(0f, maxStep.toFloat())
                                if (newValue != currentProgress) {
                                    currentOnProgressChange(newValue)
                                }
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .pointerInput(Unit) {
                    // Early check: if the slider is disabled, don't process gestures at all.
                    if (!currentEnabled) return@pointerInput

                    val widthPx = size.width.toFloat()

                    fun snapToStep(x: Float) {
                        if (!currentEnabled) return
                        val rawFraction = (x / widthPx).coerceIn(0f, 1f)
                        val snapped = (rawFraction * maxStep).roundToInt().coerceIn(0, maxStep).toFloat()
                        if (snapped != currentProgress) {
                            currentOnProgressChange(snapped)
                        }
                    }

                    detectTapGestures { offset -> snapToStep(offset.x) }
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> snapToStep(offset.x) },
                        onHorizontalDrag = { change, _ ->
                            snapToStep(change.position.x)
                            change.consume()
                        },
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val trackY = size.height / 2f
                val trackHeightPx = 8.dp.toPx()
                val trackStart = 12.dp.toPx()
                val trackEnd = size.width - 12.dp.toPx()
                val trackWidth = trackEnd - trackStart

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackStart, trackY - trackHeightPx / 2),
                    size = androidx.compose.ui.geometry.Size(trackWidth, trackHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2),
                )

                val activeEnd = trackStart + trackWidth * (progress / maxStep)
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackStart, trackY - trackHeightPx / 2),
                    size = androidx.compose.ui.geometry.Size(activeEnd - trackStart, trackHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2),
                )

                tickFractions.forEach { fraction ->
                    val cx = trackStart + trackWidth * fraction
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(cx, trackY),
                    )
                }

                val thumbX = trackStart + trackWidth * (progress / maxStep)
                drawCircle(
                    color = thumbColor,
                    radius = 10.dp.toPx(),
                    center = Offset(thumbX, trackY),
                )
            }
        }
    }
}

private fun initialAttemptsCountProgress(context: android.content.Context): Float {
    val saved = SharedPrefUtils.getReconnectAttemptsCount(context)
    for (i in ATTEMPTS_COUNT_VALUES.indices) {
        if (ATTEMPTS_COUNT_VALUES[i] >= saved) return i.toFloat()
    }
    return 2f
}

private fun initialFallbackThresholdProgress(context: android.content.Context): Float {
    val saved = SharedPrefUtils.getAutoFallbackThreshold(context)
    for (i in FALLBACK_THRESHOLD_VALUES.indices) {
        if (FALLBACK_THRESHOLD_VALUES[i] >= saved) return i.toFloat()
    }
    return 1f
}

private fun isValidDnsAddress(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    val parts = trimmed.split(".")
    if (parts.size != 4) return false
    return parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
}
