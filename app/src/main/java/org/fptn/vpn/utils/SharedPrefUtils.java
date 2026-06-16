package org.fptn.vpn.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.enums.BypassCensorshipMethod;
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

    public static boolean isPermissionsRequested(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.PERMISSIONS_REQUESTED_SHARED_PREF_KEY, false);
    }

    public static void savePermissionsRequested(Context context, boolean requested) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.PERMISSIONS_REQUESTED_SHARED_PREF_KEY, requested).apply();
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
        return SniSpoofingMode.SNI;
    }

    public static void saveSniSpoofingMode(Context context, SniSpoofingMode mode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.SNI_SPOOFING_MODE_SHARED_PREF_KEY, mode.toString()).apply();
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

}
