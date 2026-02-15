package org.fptn.vpn.core.common

object Constants {
    const val OBFUSCATION_METHOD_SHARED_PREF_KEY: String = "OBFUSCATION_METHOD_SHARED_PREF_KEY"
    const val QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY: String = "QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY"
    const val SELECTED_SERVER: String = "fptn.selected.server"
    const val SELECTED_SERVER_ID_AUTO: Int = -1
    const val START_FROM_TILE_AUTO: Int = -1000

    // NOTIFICATIONS CONSTANTS
    const val MAIN_NOTIFICATION_CHANNEL_ID = "fptnvpn-notification-main"
    const val MAIN_NOTIFICATION_CHANNEL_VERSION = "fptnvpn-notification-main-channel-version"
    const val MAIN_NOTIFICATION_CHANNEL_VERSION_NUM = 4
    const val MAIN_NOTIFICATION_CHANNEL_GROUP_ID = "fptnvpn-notification-main-group"
    const val MAIN_CONNECTED_NOTIFICATION_ID = 8975
    const val INFO_NOTIFICATION_NOTIFICATION_ID = 8979

    const val ERROR_NOTIFICATION_CHANNEL_ID = "fptnvpn-notification-error"
    const val ERROR_NOTIFICATION_CHANNEL_VERSION = "fptnvpn-notification-error-channel-version"
    const val ERROR_NOTIFICATION_CHANNEL_VERSION_NUM = 1
    const val ERROR_NOTIFICATION_CHANNEL_GROUP_ID = "fptnvpn-notification-error-group"
    const val ERROR_CONNECTED_NOTIFICATION_ID = 8989

    const val SNI_CHECKER_NOTIFICATION_CHANNEL_ID = "fptnvpn-notification-sni"
    const val SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION = "fptnvpn-notification-sni-channel-version"
    const val SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION_NUM = 1
    const val SNI_CHECKER_NOTIFICATION_CHANNEL_GROUP_ID = "fptnvpn-notification-sni-group"
    const val SNI_CHECKER_NOTIFICATION_ID = 8999

    // Shares preferences constants
    const val CURRENT_SNI_SHARED_PREF_KEY: String = "CURRENT_SNI"
    const val RESET_SELECTED_SERVER_PREF_KEY: String = "RESET_SELECTED_SERVER_PREF_KEY"
    const val APPLICATION_SHARED_PREFERENCES = "fptnvpn-shared-preferences"
    const val PERMISSIONS_REQUESTED_SHARED_PREF_KEY: String = "permissions_requested_previously"
    const val RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY: String = "RECONNECT_ON_CHANGE_IP_ENABLED"
    const val RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED_SHARED_PREF_KEY: String =
        "RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED"
    const val RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY: String = "RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY"
    const val RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY: String = "RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY"
    const val BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY: String = "BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY"

    const val PER_APP_VPN_MODE_SHARED_PREF_KEY: String = "PER_APP_VPN_MODE_SHARED_PREF_KEY"
}
