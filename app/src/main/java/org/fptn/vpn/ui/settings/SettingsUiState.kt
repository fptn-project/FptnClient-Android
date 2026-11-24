package org.fptn.vpn.ui.settings

import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.ConnectionStrategy
import org.fptn.vpn.enums.PerAppVpnMode

data class SettingsUiState(
    // Appearance & Theme
    val fontScale: Float = 1.0f,
    val isAppearanceExpanded: Boolean = true,

    // Bypass & Tunneling Strategy
    val connectionStrategy: ConnectionStrategy = ConnectionStrategy.ROLLING_TUNNEL,
    val bypassMethod: BypassCensorshipMethod = BypassCensorshipMethod.TLS_OBFUSCATION,
    val currentSni: String = "www.google.com",
    val perAppMode: PerAppVpnMode = PerAppVpnMode.EXCEPT_DISALLOWED,
    val excludedAppsCount: Int = 7,

    // Network, DNS & Reliability
    val adBlockEnabled: Boolean = true,
    val domainBlacklistEnabled: Boolean = true,
    val customDnsEnabled: Boolean = false,
    val customDnsIpv4: String = "1.1.1.1",
    val showSpeedInNotification: Boolean = false,
    val showTrafficInNotification: Boolean = false,
    val autoFallbackEnabled: Boolean = true,
    val autoFallbackThreshold: Int = 15,
    val reconnectAttemptsCount: Int = 35,
    val delayBetweenReconnect: Int = 1,
    val reconnectOnChangeNetworkType: Boolean = true,
    val reconnectOnChangeIP: Boolean = true,
    val killSwitchEnabled: Boolean = false,
    val resetSelectedServerOnDisconnect: Boolean = true,
    val quickSettingsTileAdded: Boolean = false,

    // System, Backup & Diagnostics
    val batteryOptimizationGranted: Boolean = true,
    val backgroundDataGranted: Boolean = true,
    val appVersion: String = "1.3.28",
    val buildDate: String = "24 Nov 2026",
    val isCheckingUpdates: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val showSniDialog: Boolean = false,
    val showBlacklistDialog: Boolean = false,
)
