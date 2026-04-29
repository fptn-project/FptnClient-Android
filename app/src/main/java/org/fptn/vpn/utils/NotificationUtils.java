package org.fptn.vpn.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.jetbrains.annotations.NotNull;

public class NotificationUtils {
    public static void configureNotificationChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // add main notification channel
        createOrUpdateNotificationChannel(context, notificationManager,
                Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                Constants.MAIN_NOTIFICATION_CHANNEL_VERSION,
                Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM,
                Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID,
                context.getString(R.string.notification_group_name),
                NotificationManager.IMPORTANCE_LOW, true);

        // create error notification channel
        createOrUpdateNotificationChannel(context, notificationManager,
                Constants.ERROR_NOTIFICATION_CHANNEL_ID,
                "When errors", // todo: move to strings
                Constants.ERROR_NOTIFICATION_CHANNEL_VERSION,
                Constants.ERROR_NOTIFICATION_CHANNEL_VERSION_NUM,
                Constants.ERROR_NOTIFICATION_CHANNEL_GROUP_ID,
                context.getString(R.string.errors_notification_group_name),
                NotificationManager.IMPORTANCE_LOW, true);

        // add sni checker notification channel
        createOrUpdateNotificationChannel(context, notificationManager,
                Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID,
                "Sni checker", // todo: move to strings
                Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION,
                Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_VERSION_NUM,
                Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_GROUP_ID,
                context.getString(R.string.sni_checker_notification_group_name),
                NotificationManager.IMPORTANCE_LOW, true);
    }

    private static void createOrUpdateNotificationChannel(Context context, NotificationManager notificationManager,
                                                          @NotNull String channelId,
                                                          @NotNull String channelName,
                                                          @NotNull String versionTag,
                                                          int versionNum,
                                                          @NotNull String channelGroupId,
                                                          String channelGroupName,
                                                          int importance,
                                                          boolean disableSound) {
        // add notification channel
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);
        int notificationChannelOnDevice = SharedPrefUtils.getNotificationChannelVersion(context, versionTag);
        // remove existed notification channel if their version lower than in constants
        if (notificationChannel != null && notificationChannelOnDevice < versionNum) {
            notificationManager.deleteNotificationChannel(channelId);
            notificationChannel = null;
        }

        if (notificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(channelGroupId, channelGroupName));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    channelId,
                    channelName,
                    importance);
            if (disableSound) {
                newNotificationChannel.setSound(null, null); //disable sound
            }
            newNotificationChannel.setGroup(channelGroupId);

            notificationManager.createNotificationChannel(newNotificationChannel);
            SharedPrefUtils.saveNotificationChannelVersion(context, versionTag, versionNum);
        }
    }
}
