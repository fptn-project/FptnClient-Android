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

package org.fptn.vpn.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionStrategy;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.enums.SniSpoofingMode;

import java.util.Objects;

public class SharedPrefUtils {
    /* SNI */
    public static String getSniHostname(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.CURRENT_SNI_SHARED_PREF_KEY, context.getString(R.string.default_sni));
    }

    public static void saveSniHostname(Context context, String newSni) {
        if (newSni != null && !newSni.isBlank()) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(Constants.CURRENT_SNI_SHARED_PREF_KEY, newSni).apply();
        }
    }

    /* NOTIFICATIONS */
    public static int getNotificationChannelVersion(Context context, String channelVersionTag) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(channelVersionTag, 0);
    }

    public static void saveNotificationChannelVersion(Context context, String channelVersionTag, int version) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(channelVersionTag, version).apply();
    }

    public static boolean isBatteryOptimizationRequested(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.BATTERY_OPTIMIZATION_REQUESTED_SHARED_PREF_KEY, false);
    }

    public static void saveBatteryOptimizationRequested(Context context, boolean requested) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.BATTERY_OPTIMIZATION_REQUESTED_SHARED_PREF_KEY, requested).apply();
    }

    // Xiaomi "lock in Security" can't be read back from the OS, so we remember once the user has
    // opened it and treat the step as handled from then on.
    public static boolean isXiaomiPinDone(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.XIAOMI_PIN_DONE_SHARED_PREF_KEY, false);
    }

    public static void saveXiaomiPinDone(Context context, boolean done) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.XIAOMI_PIN_DONE_SHARED_PREF_KEY, done).apply();
    }

    /* QUICK SETTINGS TILE */
    public static boolean isQuickSettingsTileRequested(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY, false);
    }

    public static void saveQuickSettingsTileRequested(Context context, boolean added) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.QUICK_SETTINGS_TILE_REQUESTED_SHARED_PREF_KEY, added).apply();
    }

    /* EXPERIMENTAL FEATURES */
    public static boolean getReconnectOnChangeNetworkTypeEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED_SHARED_PREF_KEY, true);
    }

    public static void saveReconnectOnChangeNetworkTypeEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RECONNECT_ON_CHANGE_NETWORK_TYPE_ENABLED_SHARED_PREF_KEY, enabled).apply();
    }

    public static boolean getReconnectOnChangeIPEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY, true);
    }

    public static void saveReconnectOnChangeIPEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY, enabled).apply();
    }

    public static int getReconnectAttemptsCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY, 35);
    }

    public static void saveReconnectAttemptsCount(Context context, int count) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY, count).apply();
    }


    public static int getDelayBetweenReconnect(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY, 1);
    }

    public static void saveDelayBetweenReconnect(Context context, int delayInSeconds) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY, delayInSeconds).apply();
    }

    public static boolean getResetSelectedServerEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.RESET_SELECTED_SERVER_PREF_KEY, true);
    }

    public static void saveResetSelectedServerEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RESET_SELECTED_SERVER_PREF_KEY, enabled).apply();
    }

    public static boolean getResetSelectedServerOnExceptionEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.RESET_SELECTED_SERVER_ON_EXCEPTION_PREF_KEY, false);
    }

    public static void saveResetSelectedServerOnExceptionEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RESET_SELECTED_SERVER_ON_EXCEPTION_PREF_KEY, enabled).apply();
    }

    public static boolean getAutoFallbackEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.AUTO_FALLBACK_ENABLED_PREF_KEY, true);
    }

    public static void saveAutoFallbackEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.AUTO_FALLBACK_ENABLED_PREF_KEY, enabled).apply();
    }

    public static int getAutoFallbackThreshold(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.AUTO_FALLBACK_THRESHOLD_PREF_KEY, 15);
    }

    public static void saveAutoFallbackThreshold(Context context, int count) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.AUTO_FALLBACK_THRESHOLD_PREF_KEY, count).apply();
    }

    public static BypassCensorshipMethod getBypassCensorshipMethod(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String methodName = sharedPreferences.getString(Constants.BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY, null);
        for (BypassCensorshipMethod value : BypassCensorshipMethod.values()) {
            if (Objects.equals(methodName, value.name())) {
                return value;
            }
        }
        return BypassCensorshipMethod.SNI_REALITY;
    }

    public static void saveBypassCensorshipMethod(Context context, BypassCensorshipMethod method) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.BYPASS_CENSORSHIP_METHOD_SHARED_PREF_KEY, method.toString()).apply();
    }


    public static SniSpoofingMode getSniSpoofingMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String modeName = sharedPreferences.getString(Constants.SNI_SPOOFING_MODE_SHARED_PREF_KEY, null);
        for (SniSpoofingMode value : SniSpoofingMode.values()) {
            if (Objects.equals(modeName, value.name())) {
                return value;
            }
        }
        return SniSpoofingMode.SNI_REALITY_YANDEX_26_4;
    }

    public static void saveSniSpoofingMode(Context context, SniSpoofingMode mode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.SNI_SPOOFING_MODE_SHARED_PREF_KEY, mode.toString()).apply();
    }

    public static ConnectionStrategy getConnectionStrategy(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String strategyName = sharedPreferences.getString(Constants.CONNECTION_STRATEGY_SHARED_PREF_KEY, null);
        for (ConnectionStrategy value : ConnectionStrategy.values()) {
            if (Objects.equals(strategyName, value.name())) {
                return value;
            }
        }
        return ConnectionStrategy.ROLLING_TUNNEL;
    }

    public static void saveConnectionStrategy(Context context, ConnectionStrategy strategy) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.CONNECTION_STRATEGY_SHARED_PREF_KEY, strategy.toString()).apply();
    }

    /* Per-app VPN settings */
    public static PerAppVpnMode getPerAppVPNMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String modeName = sharedPreferences.getString(Constants.PER_APP_VPN_MODE_SHARED_PREF_KEY, null);
        for (PerAppVpnMode value : PerAppVpnMode.values()) {
            if (Objects.equals(modeName, value.name())) {
                return value;
            }
        }
        return PerAppVpnMode.OFF;
    }

    public static void savePerAppVPNMode(Context context, PerAppVpnMode perAppVPNMode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.PER_APP_VPN_MODE_SHARED_PREF_KEY, perAppVPNMode.toString()).apply();
    }

    public static boolean getShowSystemApps(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.SHOW_SYSTEM_APPS_SHARED_PREF_KEY, false);
    }

    public static void saveShowSystemApps(Context context, boolean show) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.SHOW_SYSTEM_APPS_SHARED_PREF_KEY, show).apply();
    }

    /* Ad blocking */
    public static boolean getAdBlockEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.AD_BLOCK_ENABLED_PREF_KEY, true);
    }

    public static void saveAdBlockEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.AD_BLOCK_ENABLED_PREF_KEY, enabled).apply();
    }

    /* Domain blacklist */
    public static boolean getDomainBlacklistEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.DOMAIN_BLACKLIST_ENABLED_PREF_KEY, true);
    }

    public static void saveDomainBlacklistEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.DOMAIN_BLACKLIST_ENABLED_PREF_KEY, enabled).apply();
    }

    public static String getDomainBlacklistDomains(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.DOMAIN_BLACKLIST_DOMAINS_PREF_KEY, Constants.DOMAIN_BLACKLIST_DEFAULT);
    }

    public static void saveDomainBlacklistDomains(Context context, String domains) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.DOMAIN_BLACKLIST_DOMAINS_PREF_KEY, domains).apply();
    }

    /* Exclude VPN-detector apps from the tunnel */
    public static boolean getExcludeDetectorAppsEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.EXCLUDE_DETECTOR_APPS_ENABLED_PREF_KEY, true);
    }

    public static void saveExcludeDetectorAppsEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.EXCLUDE_DETECTOR_APPS_ENABLED_PREF_KEY, enabled).apply();
    }

    /* Custom DNS */
    public static boolean getCustomDnsEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.CUSTOM_DNS_ENABLED_PREF_KEY, false);
    }

    public static void saveCustomDnsEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.CUSTOM_DNS_ENABLED_PREF_KEY, enabled).apply();
    }

    public static String getCustomDnsIpv4(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.CUSTOM_DNS_IPV4_PREF_KEY, "");
    }

    public static void saveCustomDnsIpv4(Context context, String ipv4) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.CUSTOM_DNS_IPV4_PREF_KEY, ipv4).apply();
    }

    /* Speed in notification */
    public static boolean getShowSpeedInNotification(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.SHOW_SPEED_IN_NOTIFICATION_PREF_KEY, false);
    }

    public static void saveShowSpeedInNotification(Context context, boolean show) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.SHOW_SPEED_IN_NOTIFICATION_PREF_KEY, show).apply();
    }

    /* Traffic chart on home screen */
    public static boolean getShowTrafficChart(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.SHOW_TRAFFIC_CHART_PREF_KEY, true);
    }

    public static void saveShowTrafficChart(Context context, boolean show) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.SHOW_TRAFFIC_CHART_PREF_KEY, show).apply();
    }

    /* Landscape orientation. Default is device-dependent: true on tablets (sw600dp), false on phones. */
    public static boolean getAllowLandscape(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean deviceDefault = context.getResources().getBoolean(R.bool.allow_landscape_default);
        return sharedPreferences.getBoolean(Constants.ALLOW_LANDSCAPE_PREF_KEY, deviceDefault);
    }

    public static void saveAllowLandscape(Context context, boolean allow) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.ALLOW_LANDSCAPE_PREF_KEY, allow).apply();
    }

}
