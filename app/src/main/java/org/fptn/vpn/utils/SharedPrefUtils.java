package org.fptn.vpn.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;

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
}
