package org.fptn.vpn.services.snichecker;

import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class SniCheckerService extends Service {
    private static final String TAG = SniCheckerService.class.getSimpleName();

    public static final String ACTION_START = "SniCheckerService:CONNECT";
    public static final String ACTION_STOP = "SniCheckerService:DISCONNECT";
    public static final String ACTION_BIND = "SniCheckerService:BIND";
    public static final String SNI_CHECKER_POWER_LOCK = "SniCheckerService::POWER_LOCK";

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    public static final Random RANDOM = new Random();

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    private final AppDatabase appDatabase = AppDatabase.getInstance(this);

    private PowerManager.WakeLock wakeLock;


    /* Just in case we need to bind! */
    public static void bindService(Context context, ServiceConnection connection) {
        Intent intent = new Intent(context, SniCheckerService.class);
        intent.setAction(ACTION_BIND);
        context.bindService(intent, connection, BIND_AUTO_CREATE);
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SniCheckerService getService() {
            return SniCheckerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    /* JUST in case END */

    public synchronized static void startChecking(Context context, ServerEntity serverEntity) {
        Intent intent = new Intent(context, SniCheckerService.class);
        intent.setAction(ACTION_START);
        if (serverEntity != null) {
            intent.putExtra(SELECTED_SERVER, serverEntity.getId());
            context.startService(intent);
        } else {
            Log.e(TAG, "startChecking: no server selected");
        }
    }

    public synchronized static void stopChecking(Context context) {
        Intent intent = new Intent(context, SniCheckerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: intent: " + intent);
        if (intent != null) {
            String intentAction = intent.getAction();
            Log.d(TAG, "onStartCommand: intentAction: " + intentAction);

            if (ACTION_START.equalsIgnoreCase(intentAction)) {
                isRunning.set(true);
                startForegroundWithNotification("Searching the best sni");
                new Thread(() -> {
                    SniEntity foundedSni = null;
                    List<SniEntity> checkedSniEntities = new ArrayList<>();
                    List<SniEntity> sniEntitiesToCheck = appDatabase.sniDAO().getAllUnchecked();
                    if (sniEntitiesToCheck == null || sniEntitiesToCheck.isEmpty()) {
                        appDatabase.sniDAO().resetAll();
                        sniEntitiesToCheck = appDatabase.sniDAO().getAllUnchecked();
                    }

                    int allCount = appDatabase.sniDAO().count();
                    int base = allCount - sniEntitiesToCheck.size();

                    Iterator<SniEntity> iterator = sniEntitiesToCheck.iterator();
                    while (isRunning.get() && iterator.hasNext()) {
                        SniEntity current = iterator.next();
                        updateNotificationWithMessage("Check " + (base + checkedSniEntities.size()) + "/" + allCount,
                                "Current check: " + current.getSni());
                        current.setChecked(true);
                        checkedSniEntities.add(current);
                        boolean valid = checkSni(current.getSni());
                        if (valid) {
                            isRunning.set(false);
                            foundedSni = current;
                            // todo: post notification user to update sni
                            updateNotificationWithMessage("Found new: ", foundedSni.getSni());
                            SharedPrefUtils.saveSniHostname(this, foundedSni.getSni());
                            return;
                        }
                    }

                    if (!checkedSniEntities.isEmpty()) {
                        appDatabase.sniDAO().insertAll(checkedSniEntities);
                    }

                    stopForeground(STOP_FOREGROUND_DETACH);

                }).start();
            } else if (ACTION_STOP.equalsIgnoreCase(intentAction)) {
                isRunning.set(false);
            }
        }

        // if it stops - it stops
        return START_NOT_STICKY;
    }

    private boolean checkSni(String sni) {
        // todo: replace with real check
        try {
            long sleepTime = RANDOM.nextInt(3001) + 2000;
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return RANDOM.nextInt(1000) > 950;
    }

    private void startForegroundWithNotification(String title) {
        NotificationUtils.configureNotificationChannel(this);
        Notification notification = createNotification(title, "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.SNI_CHECKER_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(Constants.SNI_CHECKER_NOTIFICATION_ID, notification);
        }
    }

    private void updateNotificationWithMessage(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, message);
        notificationManager.notify(Constants.SNI_CHECKER_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.disconnect_action), disconnectPendingIntent)
                .build();
        Notification.Builder builder = new Notification.Builder(this, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setVisibility(Notification.VISIBILITY_PUBLIC) // Show this notification in its entirety on all lockscreens and while screen sharing.
                .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
                .setAutoCancel(false) // for not remove notification after press it
                .setOngoing(true) // user can't close notification (works only when screen locked)
                .addAction(actionDisconnect)
                .setContentIntent(launchMainActivityPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE); // foreground service notification behavior
        }
        return builder.build();
    }

}
