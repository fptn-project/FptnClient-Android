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

package org.fptn.vpn.services.snichecker;

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
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.bypassmethod.BypassMethodsActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class SniCheckerService extends Service {
    private static final String TAG = SniCheckerService.class.getSimpleName();

    public static final String SELECTED_SERVER = "SELECTED_SERVER";
    public static final String RESET_CHECKED_EXTRA = "RESET_CHECKED";
    public static final String BYPASS_METHOD = "BYPASS_METHOD";

    public static final String ACTION_START = "SniCheckerService:START";
    public static final String ACTION_STOP = "SniCheckerService:STOP";
    public static final String ACTION_BIND = "SniCheckerService:BIND";
    public static final String SNI_CHECKER_POWER_LOCK = "SniCheckerService::POWER_LOCK";

    public static final int SNI_BATCH_SIZE = 25;

    @Getter
    private static final MutableLiveData<SniCheckerServiceState> staticServiceState = new MutableLiveData<>(SniCheckerServiceState.INACTIVE);

    @Getter
    private final MutableLiveData<SniCheckerServiceState> serviceState = new MutableLiveData<>(SniCheckerServiceState.INACTIVE);

    @Getter
    private final MutableLiveData<String> currentSniInfo = new MutableLiveData<>();

    @Getter
    private String foundedSni = null;

    @Getter
    private final MutableLiveData<String> foundedSniLiveData = new MutableLiveData<>();

    @Getter
    private final MutableLiveData<Pair<Integer, Integer>> currentProgress = new MutableLiveData<>();

    @Getter
    private final MutableLiveData<ServerEntity> selectedServer = new MutableLiveData<>(ServerEntity.AUTO);

    @Getter
    private BypassCensorshipMethod bypassCensorshipMethod = BypassCensorshipMethod.SNI_REALITY;

    // Pending Intent for launch byPassMethodActivity when notification tapped
    private PendingIntent launchActivityPendingIntent;

    // Pending Intent to stop sni checking from notification
    private PendingIntent stopPendingIntent;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    private PowerManager.WakeLock wakeLock;

    // todo:
    //  4) not allow run vpn if sni checking in progress.

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

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure notification channels
        NotificationUtils.configureNotificationChannels(this);

        // Pending Intent for launch byPassMethodActivity when notification tapped
        launchActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BypassMethodsActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        // Pending Intent to stop sni checking from notification
        stopPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, SniCheckerService.class)
                        .setAction(SniCheckerService.ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE);


        serviceState.observeForever(staticServiceState::postValue);
    }

    public synchronized static void startChecking(Context context,
                                                  ServerEntity serverEntity,
                                                  boolean resetChecked,
                                                  BypassCensorshipMethod bypassCensorshipMethodMutableLiveData) {
        Intent intent = new Intent(context, SniCheckerService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(RESET_CHECKED_EXTRA, resetChecked);
        intent.putExtra(BYPASS_METHOD, bypassCensorshipMethodMutableLiveData.name());
        if (serverEntity != null) {
            intent.putExtra(SELECTED_SERVER, serverEntity.getId());
            context.startService(intent);
        } else {
            XLog.tag(TAG).e("Cannot start SNI check — no server selected");
        }
    }

    public synchronized static void stopChecking(Context context) {
        Intent intent = new Intent(context, SniCheckerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        XLog.tag(TAG).d("onStartCommand [action=%s]", intent != null ? intent.getAction() : "null");
        if (intent != null) {
            String intentAction = intent.getAction();

            if (ACTION_START.equalsIgnoreCase(intentAction)) {
                startForegroundWithNotification("Searching the best SNI");
                XLog.tag(TAG).i("SNI check started");

                // read params from intent
                bypassCensorshipMethod = BypassCensorshipMethod.valueOf(intent.getStringExtra(BYPASS_METHOD));
                boolean resetChecked = intent.getBooleanExtra(RESET_CHECKED_EXTRA, false);
                int serverId = intent.getIntExtra(SELECTED_SERVER, Constants.SELECTED_SERVER_ID_AUTO);

                serviceState.postValue(SniCheckerServiceState.ACTIVE);

                executorService.submit(() -> {
                    ServerEntity serverEntity = appDatabase.serverDAO().getById(serverId);
                    if (serverEntity != null) {
                        SniSpoofingMode sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(getApplicationContext());
                        SniChecker sniChecker = new SniChecker(serverEntity, bypassCensorshipMethod, sniSpoofingMode);
                        selectedServer.postValue(serverEntity);

                        acquirePowerLock();

                        foundedSni = null;

                        int currentNum = 0;
                        int allUncheckedCount = appDatabase.sniDAO().countUnchecked();
                        if (allUncheckedCount == 0 || resetChecked) {
                            XLog.tag(TAG).i("Resetting SNI check state [reset=%b, unchecked=%d]", resetChecked, allUncheckedCount);
                            appDatabase.sniDAO().resetAll();
                            allUncheckedCount = appDatabase.sniDAO().countUnchecked();
                        }

                        List<SniEntity> sniEntitiesToCheck = appDatabase.sniDAO().getUnchecked(SNI_BATCH_SIZE);
                        while (serviceState.getValue() == SniCheckerServiceState.ACTIVE
                                && !sniEntitiesToCheck.isEmpty()
                                && foundedSni == null) {

                            List<SniEntity> checkedSniEntities = new ArrayList<>();

                            Iterator<SniEntity> iterator = sniEntitiesToCheck.iterator();
                            while (serviceState.getValue() == SniCheckerServiceState.ACTIVE
                                    && iterator.hasNext()
                                    && foundedSni == null) {
                                SniEntity currentSniEntity = iterator.next();
                                currentNum++;

                                // mark current as checked
                                currentSniEntity.setChecked(true);
                                checkedSniEntities.add(currentSniEntity);

                                String currentSni = currentSniEntity.getSni();
                                currentSniInfo.postValue(currentSni);

                                currentProgress.postValue(new Pair<>(currentNum, allUncheckedCount));

                                updateNotificationWithProgress(currentSni, currentNum, allUncheckedCount);

                                // checking current
                                boolean valid = sniChecker.checkSni(currentSni);
                                if (valid) {
                                    XLog.tag(TAG).i("Found valid SNI [sni=%s, checked=%d/%d]", currentSni, currentNum, allUncheckedCount);
                                    foundedSni = currentSni;
                                }
                            }

                            if (!checkedSniEntities.isEmpty()) {
                                // save progress sni
                                try {
                                    XLog.tag(TAG).d("Saving batch to DB [count=%d]", checkedSniEntities.size());
                                    appDatabase.sniDAO().insertAll(checkedSniEntities).get();
                                } catch (ExecutionException | InterruptedException e) {
                                    XLog.tag(TAG).e("Failed to save SNI batch to DB: %s", e.getMessage());
                                }
                            }

                            // get next batch
                            sniEntitiesToCheck = appDatabase.sniDAO().getUnchecked(SNI_BATCH_SIZE);
                        }
                    } else {
                        XLog.tag(TAG).e("Server not found [id=%d] — aborting SNI check", serverId);
                    }
                    stopCheckingProcess();
                });

            } else if (ACTION_STOP.equalsIgnoreCase(intentAction)) {
                stopCheckingProcess();
            }
        }

        // if it stops - it stops
        return START_NOT_STICKY;
    }

    private void stopCheckingProcess() {
        // Release wakelock
        releasePowerLock();

        if (foundedSni != null) {
            //todo: does need add save action to notification?
            SharedPrefUtils.saveSniHostname(this, foundedSni);

            foundedSniLiveData.postValue(foundedSni);

            showResultNotification(getString(R.string.sni_found_title), getString(R.string.sni_found_message) + foundedSni);
        } else {
            showResultNotification(getString(R.string.sni_not_found_title), getString(R.string.sni_not_found_message));

            showResultNotification("SNI not found!", "Not found working SNI for server: "
                    + Optional.ofNullable(selectedServer.getValue()).map(ServerEntity::getServerInfo).orElse(""));
        }
        serviceState.postValue(SniCheckerServiceState.INACTIVE);

        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private synchronized void acquirePowerLock() {
        // release previous power lock
        releasePowerLock();
        // we need this lock so our service gets not affected by Doze Mode
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SNI_CHECKER_POWER_LOCK);
            wakeLock.acquire(5000);
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to acquire WakeLock: %s", e.getMessage());
        }
    }

    private synchronized void releasePowerLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                XLog.tag(TAG).e("Failed to release WakeLock: %s", e.getMessage());
            }
        }
    }

    private void startForegroundWithNotification(String title) {
        Notification notification = createNotificationInProgress(title, "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.SNI_CHECKER_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(Constants.SNI_CHECKER_NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotificationInProgress(String title, String message) {
        return createNotification(title, message).build();
    }

    private void updateNotificationWithProgress(String sni, int progress, int max) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification.Builder builder = createNotification("Checking SNI: " + sni, "Checking " + progress + "/" + max);
        builder.setProgress(max, progress, false);

        Notification notification = builder.build();
        notificationManager.notify(Constants.SNI_CHECKER_NOTIFICATION_ID, notification);
    }

    private Notification.Builder createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionStopChecking = new Notification.Action.Builder(null, getString(R.string.stop_sni_checking_button_label), stopPendingIntent)
                .build();
        Notification.Builder builder = new Notification.Builder(this, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sql_server_24)
                .setContentTitle(title)
                .setContentText(message)
                .setVisibility(Notification.VISIBILITY_PUBLIC) // Show this notification in its entirety on all lockscreens and while screen sharing.
                .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
                .setAutoCancel(false) // for not remove notification after press it
                .setOngoing(true) // user can't close notification (works only when screen locked)
                .addAction(actionStopChecking)
                .setContentIntent(launchActivityPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE); // foreground service notification behavior
        }
        return builder;
    }

    private void showResultNotification(String title, String message) {
/*        Notification.Action actionSaveFounded = new Notification.Action.Builder(null, getString(R.string.reconnect_action), reconnectPendingIntent)
                .build();
        Notification.Action actionContinueChecking = new Notification.Action.Builder(null, getString(R.string.reconnect_action), reconnectPendingIntent)
                .build();*/
        Notification notification = new Notification.Builder(this, Constants.SNI_CHECKER_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true) // if you tap on notification - opens activity and notification dismissed
                .setContentIntent(launchActivityPendingIntent)
                //.addActions(actionSaveFounded, actionContinueChecking)
                .setActions()
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.SNI_CHECKER_NOTIFICATION_ID, notification);
    }

}
