/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.core.common

object Constants {
    const val QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY: String = "QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY"
    const val SELECTED_SERVER: String = "fptn.selected.server"
    const val SELECTED_SERVER_ID_AUTO: Int = -1
    const val START_FROM_TILE_AUTO: Int = -1000

    // NOTIFICATIONS CONSTANTS
    const val MAIN_NOTIFICATION_CHANNEL_ID = "fptnvpn-notification-main"
    const val MAIN_NOTIFICATION_CHANNEL_VERSION = "fptnvpn-notification-main-channel-version"
    const val MAIN_NOTIFICATION_CHANNEL_VERSION_NUM = 6
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
    const val RESET_SELECTED_SERVER_ON_EXCEPTION_PREF_KEY: String = "RESET_SELECTED_SERVER_ON_EXCEPTION_PREF_KEY"
    const val APPLICATION_SHARED_PREFERENCES = "fptnvpn-shared-preferences"
    const val BATTERY_OPTIMIZATION_REQUESTED_SHARED_PREF_KEY: String = "battery_optimization_requested"
    const val XIAOMI_PIN_DONE_SHARED_PREF_KEY: String = "xiaomi_pin_done"
    const val RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY: String = "RECONNECT_ON_CHANGE_IP_ENABLED_V2"
    const val RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED_SHARED_PREF_KEY: String =
        "RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED"
    const val RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY: String = "RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY_V2"
    const val RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY: String = "RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY_V2"
    const val BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY: String = "BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY"

    const val SNI_SPOOFING_MODE_SHARED_PREF_KEY: String = "SNI_SPOOFING_MODE_SHARED_PREF_KEY_V2"
    const val PER_APP_VPN_MODE_SHARED_PREF_KEY: String = "PER_APP_VPN_MODE_SHARED_PREF_KEY"
    const val SHOW_SYSTEM_APPS_SHARED_PREF_KEY: String = "SHOW_SYSTEM_APPS_SHARED_PREF_KEY"
    const val AUTO_FALLBACK_ENABLED_PREF_KEY: String = "AUTO_FALLBACK_TO_ALL_SERVERS_ENABLED_V1"
    const val AUTO_FALLBACK_THRESHOLD_PREF_KEY: String = "AUTO_FALLBACK_THRESHOLD_V1"
    const val AD_BLOCK_ENABLED_PREF_KEY: String = "AD_BLOCK_ENABLED_V2"
    const val CUSTOM_DNS_ENABLED_PREF_KEY: String = "CUSTOM_DNS_ENABLED_V1"
    const val CUSTOM_DNS_IPV4_PREF_KEY: String = "CUSTOM_DNS_IPV4_V1"
    const val SHOW_SPEED_IN_NOTIFICATION_PREF_KEY: String = "SHOW_SPEED_IN_NOTIFICATION_V1"
    const val SHOW_TRAFFIC_CHART_PREF_KEY: String = "SHOW_TRAFFIC_CHART_V1"
}
