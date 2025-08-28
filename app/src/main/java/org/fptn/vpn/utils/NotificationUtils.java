package org.fptn.vpn.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;

public class NotificationUtils {

    public static void configureNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        // remove existed notification channel if their version lower than in constants
        int notificationChannelOnDevice = SharedPrefUtils.getNotificationChannelVersion(context);
        if (notificationChannel != null && notificationChannelOnDevice < Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM) {
            notificationManager.deleteNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
            notificationChannel = null;
        }

        if (notificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID, context.getString(R.string.notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            newNotificationChannel.setGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID);
            newNotificationChannel.setSound(null, null); //disable sound

            notificationManager.createNotificationChannel(newNotificationChannel);
            SharedPrefUtils.saveNotificationChannelVersion(context, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM);
        }
    }
}
