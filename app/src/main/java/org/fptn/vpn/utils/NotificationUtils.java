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

        // add main notification channel
        NotificationChannel mainNotificationChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        int mainNotificationChannelOnDevice = SharedPrefUtils.getNotificationChannelVersion(context, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION);
        // remove existed notification channel if their version lower than in constants
        if (mainNotificationChannel != null && mainNotificationChannelOnDevice < Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM) {
            notificationManager.deleteNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
            mainNotificationChannel = null;
        }

        if (mainNotificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID, context.getString(R.string.notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            newNotificationChannel.setGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID);
            newNotificationChannel.setSound(null, null); //disable sound

            notificationManager.createNotificationChannel(newNotificationChannel);
            SharedPrefUtils.saveNotificationChannelVersion(context, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM);
        }

        // add error notification channel
        NotificationChannel errorNotificationChannel = notificationManager.getNotificationChannel(Constants.ERROR_NOTIFICATION_CHANNEL_ID);
        int errorNotificationChannelOnDevice = SharedPrefUtils.getNotificationChannelVersion(context, Constants.ERROR_NOTIFICATION_CHANNEL_VERSION);
        // remove existed notification channel if their version lower than in constants
        if (errorNotificationChannel != null && errorNotificationChannelOnDevice < Constants.ERROR_NOTIFICATION_CHANNEL_VERSION_NUM) {
            notificationManager.deleteNotificationChannel(Constants.ERROR_NOTIFICATION_CHANNEL_ID);
            errorNotificationChannel = null;
        }

        if (errorNotificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.ERROR_NOTIFICATION_CHANNEL_GROUP_ID, context.getString(R.string.errors_notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.ERROR_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.errors_notification_group_name),
                    NotificationManager.IMPORTANCE_HIGH);
            newNotificationChannel.setSound(null, null); //disable sound
            newNotificationChannel.setGroup(Constants.ERROR_NOTIFICATION_CHANNEL_GROUP_ID);

            notificationManager.createNotificationChannel(newNotificationChannel);
            SharedPrefUtils.saveNotificationChannelVersion(context, Constants.ERROR_NOTIFICATION_CHANNEL_VERSION, Constants.ERROR_NOTIFICATION_CHANNEL_VERSION_NUM);
        }

        // add sni checker notifications
        NotificationChannel sniCheckerNotificationChannel = notificationManager.getNotificationChannel(Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID);
        int sniCheckerNotificationChannelOnDevice = SharedPrefUtils.getNotificationChannelVersion(context, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION);
        // remove existed notification channel if their version lower than in constants
        if (sniCheckerNotificationChannel != null && sniCheckerNotificationChannelOnDevice < Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION_NUM) {
            notificationManager.deleteNotificationChannel(Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID);
            sniCheckerNotificationChannel = null;
        }

        if (sniCheckerNotificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_GROUP_ID, context.getString(R.string.errors_notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.sni_checker_notification_group_name),
                    NotificationManager.IMPORTANCE_HIGH);
            newNotificationChannel.setSound(null, null); //disable sound
            newNotificationChannel.setGroup(Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_GROUP_ID);

            notificationManager.createNotificationChannel(newNotificationChannel);
            SharedPrefUtils.saveNotificationChannelVersion(context, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION_NUM);
        }
    }
}
