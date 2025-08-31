package org.fptn.vpn.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;

public class SharedPrefUtils {
    private static final String TAG = SharedPrefUtils.class.getSimpleName();

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

    public static void resetToDefaultSniHostname(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.CURRENT_SNI_SHARED_PREF_KEY, context.getString(R.string.default_sni)).apply();
    }

    /* NOTIFICATIONS */
    public static int getNotificationChannelVersion(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, 0);
    }

    public static void saveNotificationChannelVersion(Context context, int version) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, version).apply();
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
        return sharedPreferences.getBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY, false);
    }

    public static void saveReconnectOnChangeIPEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLED_SHARED_PREF_KEY, enabled).apply();
    }

    public static int getReconnectAttemptsCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY, 5);
    }

    public static void saveReconnectAttemptsCount(Context context, int count) {
        Log.d(TAG, "saveReconnectAttemptsCount: " + count);

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.RECONNECT_ATTEMPTS_COUNT_SHARED_PREF_KEY, count).apply();
    }


    public static int getDelayBetweenReconnect(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY, 2);
    }

    public static void saveDelayBetweenReconnect(Context context, int delayInSeconds) {
        Log.d(TAG, "saveDelayBetweenReconnect: " + delayInSeconds);
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.RECONNECT_DELAY_BETWEEN_SHARED_PREF_KEY, delayInSeconds).apply();
    }
}
