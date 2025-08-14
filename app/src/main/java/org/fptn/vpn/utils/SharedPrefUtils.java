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

    /* RECONNECTION */
    public static boolean getReconnectEnable(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLE_SHARED_PREF_KEY, false);
    }

    public static void saveReconnectEnable(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Constants.RECONNECT_ON_CHANGE_IP_ENABLE_SHARED_PREF_KEY, enabled).apply();
    }

    /* NOTIFICATIONS */
    public static int getNotificationChannelVersion(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, 0);
    }

    public static void saveNotificationChannelVersion(Context context, int version) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM).apply();
    }
}
