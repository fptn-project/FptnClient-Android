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
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.ui.MainActivity;
import org.fptn.vpn.ui.navigation.AppRoute;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
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

    /** Non-Lombok accessor so Kotlin (Compose) code can read the static service state. */
    public static SniCheckerServiceState getStaticState() {
        return staticServiceState.getValue();
    }

    /** Non-Lombok accessor so Kotlin (Compose) code can observe the static service state LiveData. */
    public static MutableLiveData<SniCheckerServiceState> getStaticServiceStateLiveData() {
        return staticServiceState;
    }

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
                MainActivity.intentForRoute(this, AppRoute.BYPASS_METHODS),
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
        ConnectionState vpnState = FptnTileService.getServiceStateMutableLiveData().getValue();
        if (vpnState != null && vpnState.isActiveState()) {
            XLog.tag(TAG).w("Refusing to start SNI check — VPN is active");
            return;
        }
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
        // No-op if the SNI checker isn't actually running — otherwise sending ACTION_STOP would
        // start the service just to immediately call stopCheckingProcess(), which would show
        // "SNI not found" notifications for a search that never happened (e.g. on logout).
        if (getStaticState() != SniCheckerServiceState.ACTIVE) {
            XLog.tag(TAG).i("stopChecking: service not active — nothing to do");
            return;
        }
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
                // Security gate (early): refuse to start if there are no servers AND no tokens in
                // the DB. This can happen when ACTION_START arrives right after a logout cleared
                // the tables — serverDAO().getById(serverId) would return null, the worker would
                // "abort SNI check" silently, and stopCheckingProcess() (with the now-ACTIVE state)
                // would post a misleading "SNI not found" notification. Bounce off a worker with a
                // 2s timeout — onStartCommand runs on the main thread, and AppDatabase forbids
                // main-thread queries.
                boolean earlySessionEstablished = false;
                FutureTask<Boolean> earlyCheck = new FutureTask<>(() ->
                        appDatabase.serverDAO().getCountSync() > 0 && appDatabase.tokenDAO().getCount() > 0);
                executorService.execute(earlyCheck);
                try {
                    earlySessionEstablished = earlyCheck.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    XLog.tag(TAG).w("Early session check failed (%s) — aborting SNI start",
                            e.getClass().getSimpleName());
                    earlyCheck.cancel(true);
                    stopSelf();
                    return START_NOT_STICKY;
                }
                if (!earlySessionEstablished) {
                    XLog.tag(TAG).w("Refusing to start SNI check — session not established");
                    stopSelf();
                    return START_NOT_STICKY;
                }

                startForegroundWithNotification("Searching the best SNI");
                XLog.tag(TAG).i("SNI check started");

                // read params from intent
                bypassCensorshipMethod = BypassCensorshipMethod.valueOf(intent.getStringExtra(BYPASS_METHOD));
                boolean resetChecked = intent.getBooleanExtra(RESET_CHECKED_EXTRA, false);
                int serverId = intent.getIntExtra(SELECTED_SERVER, Constants.SELECTED_SERVER_ID_AUTO);

                // setValue (not postValue) — onStartCommand runs on the main thread, and the
                // worker launched below checks serviceState.getValue() == ACTIVE in its while loop.
                // postValue would deliver ACTIVE asynchronously via the main looper; on a very fast
                // device the worker could enter the loop before the postValue was dispatched, see
                // INACTIVE, skip the loop entirely, fall through to stopCheckingProcess() and post
                // a misleading "SNI not found" notification. setValue updates the underlying
                // volatile field synchronously so the worker observes ACTIVE immediately.
                serviceState.setValue(SniCheckerServiceState.ACTIVE);

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

        // Only post result notifications if a search was actually running. ACTION_STOP can reach
        // the service even when the checker was never started (e.g. logout calls stopChecking()
        // defensively) — in that case foundedSni is null simply because no search ever ran, not
        // because the search ran out of candidates. Showing "SNI not found" then is misleading.
        boolean wasActive = serviceState.getValue() == SniCheckerServiceState.ACTIVE;

        if (foundedSni != null) {
            //todo: does need add save action to notification?
            SharedPrefUtils.saveSniHostname(this, foundedSni);

            foundedSniLiveData.postValue(foundedSni);

            if (wasActive) {
                showResultNotification(getString(R.string.sni_found_title), getString(R.string.sni_found_message) + foundedSni);
            }
        } else if (wasActive) {
            // Single localized notification (the hardcoded English duplicate was removed).
            showResultNotification(getString(R.string.sni_not_found_title), getString(R.string.sni_not_found_message));
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
