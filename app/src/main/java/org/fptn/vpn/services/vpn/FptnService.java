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

package org.fptn.vpn.services.vpn;

import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER;
import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER_ID_AUTO;
import static org.fptn.vpn.core.common.Constants.START_FROM_TILE_AUTO;
import static org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.domainblocker.DomainBlocker;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.NetworkType;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.services.websocket.DnsServers;
import org.fptn.vpn.utils.NetworkUtils;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.perappvpn.AppInfo;
import org.fptn.vpn.services.speedtest.SpeedTestResult;
import org.fptn.vpn.services.speedtest.SpeedTestUtils;
import org.fptn.vpn.views.splash.SplashActivity;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import kotlin.Pair;
import kotlin.Triple;
import lombok.Getter;

@SuppressLint("VpnServicePolicy")
public class FptnService extends VpnService {
    private static final String TAG = FptnService.class.getSimpleName();

    public static final String ACTION_CONNECT = "FptnService:CONNECT";
    public static final String ACTION_DISCONNECT = "FptnService:DISCONNECT";
    public static final String ACTION_BIND = "FptnService:BIND";
    public static final String FPTN_SERVICE_POWER_LOCK = "FptnService::POWER_LOCK";

    public static final String DISCONNECT_REASON_SYSTEM_REVOKED = "reason:system_revoked";
    public static final String DISCONNECT_REASON_CLOSED_UNEXPECTEDLY = "reason:closed_unexpectedly";
    public static final String DISCONNECT_REASON_UNEXPECTED_ERROR = "reason:unexpected_error";

    private final AtomicReference<FptnConnection> activeConnection = new AtomicReference<>();
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private final AtomicInteger remainingFallbackBudget = new AtomicInteger(0);

    private ConnectivityManager.NetworkCallback networkWaitCallback;
    private volatile int pendingServerId = SELECTED_SERVER_ID_AUTO;

    private volatile DnsServers cachedDnsServers = null;
    private volatile int cachedDnsServersServerId = -1;

    private volatile boolean restoringSession = false;
    // Number of reconnect attempts on the current server during a restore episode,
    // before escalating to an all-server scan ("change server on connection loss").
    private final AtomicInteger restoreRetryCount = new AtomicInteger(0);

    // Grace period to wait for a NET_CAPABILITY_VALIDATED network after it appears,
    // before attempting a connection anyway. Kept short so we never rely on Android's
    // validation probe (which is routinely blocked in censored networks) — it is only a hint.
    private static final long VALIDATED_GRACE_PERIOD_MS = 2000L;
    private final Handler restoreHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean waitProceeded = new AtomicBoolean(false);
    private final Runnable proceedAfterGraceRunnable = this::proceedFromWait;

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    // Pending Intent to reconnect from notification
    private PendingIntent reconnectPendingIntent;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private volatile String pendingRevokeReason = null;

    @Getter
    private final MutableLiveData<FptnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(FptnServiceState.INITIAL);
    @Getter
    private final MutableLiveData<Triple<String, String, Long>> speedAndDurationMutableLiveData = new MutableLiveData<>();
    @Getter
    private final MutableLiveData<Pair<Long, Long>> trafficBytesLiveData = new MutableLiveData<>(new Pair<>(0L, 0L));
    @Getter
    private final MutableLiveData<long[]> rawSpeedBpsLiveData = new MutableLiveData<>(new long[]{0L, 0L});

    @Getter
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    private Observer<FptnServiceState> serviceStateObserver;

    private AppDatabase appDatabase;

    /**
     * LocalBinder - just the way to give HomeActivity link on FptnService object
     */
    private final IBinder binder = new LocalBinder();
    private volatile Future<?> submittedConnectionAttempt;

    public static void bindService(Context context, ServiceConnection connection) {
        Intent intent = new Intent(context, FptnService.class);
        intent.setAction(ACTION_BIND);
        context.bindService(intent, connection, BIND_AUTO_CREATE);
    }

    public class LocalBinder extends Binder {
        public FptnService getService() {
            return FptnService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    /* binder part END */

    public void updateSpeedInfo(String downloadSpeed, String uploadSpeed, long duration, long totalDownload, long totalUpload, long downloadBps, long uploadBps) {
        if (serviceStateMutableLiveData.getValue().getConnectionState() == ConnectionState.CONNECTED) {
            if (SharedPrefUtils.getShowSpeedInNotification(getApplication())) {
                updateNotificationWithMessage(
                        String.format("%s %s", getString(R.string.connected_to), getActionConnectServerInfo()),
                        String.format(getString(R.string.download_upload_speed_pattern), downloadSpeed, uploadSpeed)
                );
            }

            speedAndDurationMutableLiveData.postValue(new Triple<>(downloadSpeed, uploadSpeed, duration));
            trafficBytesLiveData.postValue(new Pair<>(totalDownload, totalUpload));
            rawSpeedBpsLiveData.postValue(new long[]{downloadBps, uploadBps});
        }
    }

    public void disconnectSilently(int senderConnectionId) {
        FptnConnection current = activeConnection.get();
        if (current == null || current.getConnectionId() != senderConnectionId) {
            return;
        }
        XLog.tag(TAG).w("DISCONNECT REASON: silent disconnect requested [connectionId=%d] — TUN gone or foreign VPN detected", senderConnectionId);
        executorService.submit(() -> disconnect(null, getString(R.string.disconnect_reason_vpn_conflict)));
    }

    public void sendExceptionToService(PVNClientException exception, int senderConnectionId) {
        FptnConnection current = activeConnection.get();
        if (current == null || current.getConnectionId() != senderConnectionId) {
            XLog.tag(TAG).d("Ignoring stale exception [from=id%d, active=%s, code=%s]",
                    senderConnectionId,
                    current != null ? String.valueOf(current.getConnectionId()) : "none",
                    exception.errorCode);
            return;
        }
        if (Objects.equals(exception.errorCode, ErrorCode.FALLBACK_TO_ALL_SERVERS)) {
            submittedConnectionAttempt = executorService.submit(this::handleFallbackToAllServers);
            return;
        }
        if (restoringSession && Objects.equals(exception.errorCode, ErrorCode.CONNECT_TO_SERVER_ERROR)) {
            int failedServerId = current.getServerEntity().getId();
            // Network dropped again — stay in the waiting loop rather than giving up.
            if (!NetworkUtils.isOnline(connectivityManager)) {
                XLog.tag(TAG).i("Reconnect couldn't re-establish, offline — waiting for network [server=%d]",
                        failedServerId);
                enterWaitingForNetwork(failedServerId);
                return;
            }
            // Finite attempts budget spent — the episode is over, report honestly.
            if (remainingFallbackBudget.get() <= 0) {
                XLog.tag(TAG).e("Reconnect: attempts budget exhausted — giving up");
                restoringSession = false;
                restoreRetryCount.set(0);
                disconnect(new PVNClientException(ErrorCode.RECONNECTING_FAILED));
                showReconnectionFailedNotification();
                return;
            }
            int attempt = restoreRetryCount.incrementAndGet();
            boolean autoFallbackEnabled = SharedPrefUtils.getAutoFallbackEnabled(this)
                    && !current.getServerEntity().IsAuto();
            int maxReconnectCount = SharedPrefUtils.getReconnectAttemptsCount(this);
            int sameServerAttempts = autoFallbackEnabled
                    ? Math.min(SharedPrefUtils.getAutoFallbackThreshold(this), maxReconnectCount)
                    : maxReconnectCount;
            // Give the current server a fair number of retries before switching servers.
            // A single early failure (e.g. the link came up but isn't usable yet) must not
            // escalate straight into an all-server scan and a terminal disconnect.
            if (attempt <= sameServerAttempts) {
                XLog.tag(TAG).i("Reconnect: attempt %d/%d on server %d failed — retrying same server",
                        attempt, sameServerAttempts, failedServerId);
                scheduleRestoreRetry(failedServerId, attempt);
                return;
            }
            if (autoFallbackEnabled) {
                XLog.tag(TAG).i("Reconnect: server %d exhausted after %d attempts — scanning for a working server",
                        failedServerId, attempt);
                setActiveConnection(null);
                submittedConnectionAttempt = executorService.submit(this::handleFallbackToAllServers);
                return;
            }
            // Server-change disabled (or Auto server): honour the reconnect-attempts budget and stop.
            XLog.tag(TAG).e("Reconnect: server %d unreachable after %d attempts and fallback disabled — giving up",
                    failedServerId, attempt);
            restoringSession = false;
            restoreRetryCount.set(0);
            disconnect(exception);
            return;
        }
        restoringSession = false;
        restoreRetryCount.set(0);
        disconnect(exception);
        if (Objects.equals(exception.errorCode, ErrorCode.RECONNECTING_FAILED)) {
            showReconnectionFailedNotification();
        }
    }

    public void updateConnectionState(ConnectionState connectionState, int reconnectionCount, int senderConnectionId) {
        updateConnectionState(connectionState, reconnectionCount, senderConnectionId, null);
    }

    public void updateConnectionState(ConnectionState connectionState, int reconnectionCount, int senderConnectionId, String disconnectReason) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            FptnConnection current = activeConnection.get();
            if (current != null && current.getConnectionId() != senderConnectionId) {
                XLog.tag(TAG).d("Ignoring stale DISCONNECTED [from=id%d, active=id%d]",
                        senderConnectionId, current.getConnectionId());
                return;
            }
        }
        switchState(connectionState, reconnectionCount, disconnectReason);
    }

    public void cacheDnsServers(int serverId, DnsServers dnsServers) {
        cachedDnsServers = dnsServers;
        cachedDnsServersServerId = serverId;
    }

    /* Static methods to start/stop service */
    public synchronized static void startToConnect(Context context, ServerEntity serverEntity) {
        Intent intent = new Intent(context, FptnService.class);
        intent.setAction(ACTION_CONNECT);
        if (serverEntity != null) {
            intent.putExtra(SELECTED_SERVER, serverEntity.getId());
        }
        context.startService(intent);
    }

    public synchronized static void startToConnect(Context context) {
        Intent intent = new Intent(context, FptnService.class);
        intent.setAction(ACTION_CONNECT);
        // Now it method called only from FptnTileService
        intent.putExtra(SELECTED_SERVER, START_FROM_TILE_AUTO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // error occurs only on start from tile
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public synchronized static void startToConnectFromTile(android.service.quicksettings.TileService tileService) {
        Intent intent = new Intent(tileService, FptnService.class);
        intent.setAction(ACTION_CONNECT);
        intent.putExtra(SELECTED_SERVER, START_FROM_TILE_AUTO);
        tileService.startForegroundService(intent);
    }

    public synchronized static void startToDisconnect(Context context) {
        Intent intent = new Intent(context, FptnService.class);
        intent.setAction(ACTION_DISCONNECT);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        XLog.tag(TAG).i("Service created [manufacturer=%s, brand=%s, model=%s, sdk=%d]",
                Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.VERSION.SDK_INT);

        // Configure notification channels
        NotificationUtils.configureNotificationChannels(this);

        // Get database instance (this need context! context may not exist earlier!)
        appDatabase = AppDatabase.getInstance(this);

        // pending intent for open MainActivity on tap
        launchMainActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SplashActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        // pending intent for disconnect button in connected notification
        disconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, FptnService.class)
                        .setAction(FptnService.ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE);

        // pending intent for reconnect button in error notification
        reconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, FptnService.class)
                        .setAction(FptnService.ACTION_CONNECT),
                PendingIntent.FLAG_IMMUTABLE);

        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        serviceStateObserver = (fptnServiceState) -> {
            // setValue (not postValue) — observer runs on main thread via observeForever,
            // so the static LiveData must be updated synchronously before the requestListeningState
            // IPC fires onStartListening(), otherwise getValue() there reads stale state.
            ConnectionState newState = fptnServiceState.getConnectionState();
            FptnTileService.getServiceStateMutableLiveData().setValue(newState);
            // Only notify the tile for terminal states. Transient states (CONNECTING, RECONNECTING,
            // SEARCH_SNI) are skipped to prevent rapid back-to-back requestListeningState() calls
            // from being rate-limited/deduplicated by the system — which would cause the subsequent
            // DISCONNECTED call to be dropped and the tile to stay lit after a failed connection.
            if (newState == ConnectionState.CONNECTED || newState == ConnectionState.DISCONNECTED
                    || newState == ConnectionState.WAITING_FOR_NETWORK) {
                notifyTileListeningState();
            }
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
        //send initial value
        FptnTileService.getServiceStateMutableLiveData().setValue(serviceStateMutableLiveData.getValue().getConnectionState());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // service restarted by OS (e.g. TECNO/OEM aggressive process management)
        if (intent == null) {
            XLog.tag(TAG).w("Received null intent — service restarted by system; reconnecting");
            startForegroundWithNotification(getString(R.string.connecting));
            // This IS a reconnection (the OS killed us mid-session): recover through the
            // restore cycle instead of dying on the first failed scan — the network is often
            // not usable yet right after a process kill.
            restoringSession = true;
            // Fresh process: the episode budget field is still 0 — fund the recovery episode,
            // otherwise the first escalation to a scan sees an exhausted budget and dies.
            remainingFallbackBudget.set(SharedPrefUtils.getReconnectAttemptsCount(this));
            startConnectionAttempt(SELECTED_SERVER_ID_AUTO);
            return START_NOT_STICKY;
        }

        ConnectionState currentState = serviceStateMutableLiveData.getValue().getConnectionState();
        boolean isActiveState = currentState.isActiveState();
        XLog.tag(TAG).i("Received command [action=%s, state=%s]", intent.getAction(), currentState);

        if (ACTION_CONNECT.equals(intent.getAction()) && !isActiveState) {
            restoringSession = false;
            restoreRetryCount.set(0);
            // Drop ALL pending restore work (grace runnable AND delayed retries) — a stale
            // retry firing into the new session would race it with the old server id.
            restoreHandler.removeCallbacksAndMessages(null);
            if (submittedConnectionAttempt != null && !submittedConnectionAttempt.isDone()) {
                XLog.tag(TAG).w("Ignoring CONNECT — connection attempt already in progress [state=%s]", currentState);
                return START_STICKY;
            }
            startForegroundWithNotification(getString(R.string.connecting));

            if (!NetworkUtils.isOnline(connectivityManager)) {
                XLog.tag(TAG).i("No internet — entering WAITING_FOR_NETWORK state");
                // This IS the "reconnect on network loss" scenario the attempts setting is for:
                // when the network returns half-alive, the first failed scan must go through the
                // recovery cycle (retry/scan per the configured budget), not die with a terminal
                // "all servers unreachable" — with restoringSession=false it did exactly that.
                restoringSession = true;
                remainingFallbackBudget.set(SharedPrefUtils.getReconnectAttemptsCount(this));
                pendingServerId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);
                updateNotificationWithMessage(getString(R.string.waiting_for_network), "");
                setConnectionState(ConnectionState.WAITING_FOR_NETWORK, null);
                registerNetworkWaitCallback();
                return START_STICKY;
            }

            startConnectionAttempt(intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO));

        } else if (ACTION_DISCONNECT.equals(intent.getAction()) && isActiveState) {
            XLog.tag(TAG).i("User-initiated disconnect");

            if (submittedConnectionAttempt != null && !submittedConnectionAttempt.isDone()) {
                submittedConnectionAttempt.cancel(true);
                submittedConnectionAttempt = null;
                XLog.tag(TAG).i("Canceled connection attempt");
            }

            executorService.submit(() -> disconnect());
        } else if (ACTION_CONNECT.equals(intent.getAction())) {
            XLog.tag(TAG).w("Ignoring CONNECT — already in state [%s]", currentState);
        } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
            XLog.tag(TAG).w("Ignoring DISCONNECT — service not active [%s]", currentState);
        }

        return START_STICKY;
    }

    private void setSelectedServer(Integer serverId) throws ExecutionException, InterruptedException {
        appDatabase.serverDAO().setSelected(serverId);
    }

    private void resetSelectedServer() throws ExecutionException, InterruptedException {
        appDatabase.serverDAO().resetSelected();
    }

    private ServerEntity getSelectedServer() throws ExecutionException, InterruptedException {
        return appDatabase.serverDAO().getSelected();
    }

    @Override
    public void onRevoke() {
        XLog.tag(TAG).i("VPN permission revoked — another VPN took over, disconnecting");
        pendingRevokeReason = getString(R.string.disconnect_reason_vpn_revoked);
        executorService.submit(() -> disconnect(null, pendingRevokeReason));
    }

    @Override
    public void onDestroy() {
        XLog.tag(TAG).i("Service destroyed [state=%s]", serviceStateMutableLiveData.getValue().getConnectionState());

        // Sync tile synchronously before cleanup — postValue inside disconnect() won't reach
        // the observer once it's removed below (both run on main thread).
        FptnTileService.getServiceStateMutableLiveData().setValue(ConnectionState.DISCONNECTED);
        notifyTileListeningState();

        // If revoked, post disconnect reason to UI synchronously before observer is removed
        if (pendingRevokeReason != null) {
            serviceStateMutableLiveData.setValue(FptnServiceState.builder()
                    .connectionState(ConnectionState.DISCONNECTED)
                    .disconnectReason(pendingRevokeReason)
                    .build());
        }

        disconnect();

        if (serviceStateObserver != null) {
            serviceStateMutableLiveData.removeObserver(serviceStateObserver);
            serviceStateObserver = null;
        }
    }


    private synchronized void registerNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);

                FptnConnection currentConnection = activeConnection.get();
                ConnectionState connectionState = Optional.ofNullable(serviceStateMutableLiveData.getValue())
                        .map(FptnServiceState::getConnectionState).orElse(null);
                if (currentConnection != null && connectionState == ConnectionState.CONNECTED) {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    NetworkCapabilities activeNetworkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (activeNetworkCapabilities != null && activeNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        boolean needReconnectByNetworkType = false;
                        boolean reconnectOnChangeNetworkTypeEnabled = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(FptnService.this);
                        if (reconnectOnChangeNetworkTypeEnabled) {
                            NetworkType activeNetworkType = NetworkUtils.getNetworkType(activeNetworkCapabilities);
                            needReconnectByNetworkType = activeNetworkType != currentConnection.getCurrentNetworkType();
                            if (needReconnectByNetworkType) {
                                XLog.tag(TAG).i("Network type changed [%s -> %s] — triggering reconnect",
                                        currentConnection.getCurrentNetworkType(), activeNetworkType);
                                currentConnection.setCurrentNetworkType(activeNetworkType);
                            }
                        }

                        boolean needReconnectByIp = false;
                        boolean reconnectOnChangeIPEnabled = SharedPrefUtils.getReconnectOnChangeIPEnabled(FptnService.this);
                        if (reconnectOnChangeIPEnabled) {
                            String currentIPAddress = NetworkUtils.getCurrentIPAddress();
                            needReconnectByIp = !Objects.equals(currentIPAddress, currentConnection.getCurrentIPAddress());
                            if (needReconnectByIp) {
                                XLog.tag(TAG).i("External IP changed [%s -> %s] — triggering reconnect",
                                        currentConnection.getCurrentIPAddress(), currentIPAddress);
                                currentConnection.setCurrentIPAddress(currentIPAddress);
                            }
                        }

                        if (needReconnectByNetworkType || needReconnectByIp) {
                            currentConnection.onNetworkChanged();
                        }
                    }
                }
            }
        };

        connectivityManager.registerNetworkCallback(NetworkUtils.createNetworkRequest(), networkCallback);
    }

    private synchronized void unregisterNetworkCallback() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    public void enterWaitingForNetwork(int serverId) {
        XLog.tag(TAG).i("Network offline — entering WAITING_FOR_NETWORK [pendingServerId=%d]", serverId);
        restoringSession = true;
        // A genuine network-loss episode gives the server a fresh set of retries once it returns.
        restoreRetryCount.set(0);
        // Drop ALL pending restore work: a delayed same-server retry firing while we sit in
        // WAITING_FOR_NETWORK would attempt offline and churn for nothing.
        restoreHandler.removeCallbacksAndMessages(null);

        String serverInfo = getActionConnectServerInfo(); // capture before teardown nulls the connection
        pendingServerId = serverId;
        updateNotificationWithMessage(getString(R.string.waiting_for_network), serverInfo);
        setActiveConnection(null);
        serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                .connectionState(ConnectionState.WAITING_FOR_NETWORK)
                .serverInfo(serverInfo)
                .build());
        registerNetworkWaitCallback();
    }

    private synchronized void registerNetworkWaitCallback() {
        unregisterNetworkWaitCallback();
        waitProceeded.set(false);
        restoreHandler.removeCallbacks(proceedAfterGraceRunnable);
        networkWaitCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    XLog.tag(TAG).i("Network available and validated — resuming connection");
                    proceedFromWait();
                } else {
                    // The link is up but Android hasn't validated it yet. Don't block on validation —
                    // it is frequently unreachable under censorship. Wait a short grace period for
                    // validation to arrive (onCapabilitiesChanged), then attempt anyway.
                    XLog.tag(TAG).i("Network available but not yet validated — attempting in up to %dms", VALIDATED_GRACE_PERIOD_MS);
                    restoreHandler.postDelayed(proceedAfterGraceRunnable, VALIDATED_GRACE_PERIOD_MS);
                }
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    XLog.tag(TAG).i("Network validated — resuming connection");
                    proceedFromWait();
                }
            }
        };
        connectivityManager.registerNetworkCallback(NetworkUtils.createNetworkRequest(), networkWaitCallback);
        XLog.tag(TAG).i("Waiting for network connectivity [pendingServerId=%d]", pendingServerId);
    }

    // Resume a pending connection attempt once — guarded so the validated-now path and the
    // grace-period timeout can't both fire.
    private void proceedFromWait() {
        if (!waitProceeded.compareAndSet(false, true)) {
            return;
        }
        restoreHandler.removeCallbacks(proceedAfterGraceRunnable);
        unregisterNetworkWaitCallback();
        updateNotificationWithMessage(getString(R.string.connecting), "");
        startConnectionAttempt(pendingServerId);
    }

    // Sticky retry of the same server during a restore episode: schedule another attempt after
    // the configured delay instead of tearing the session down. Keeps restoringSession true so a
    // subsequent failure re-enters the restore logic (retry again, escalate, or wait for network).
    private void scheduleRestoreRetry(int failedServerId, int attempt) {
        String serverInfo = getActionConnectServerInfo(); // capture before teardown nulls the connection
        setActiveConnection(null);
        pendingServerId = failedServerId;
        int delaySeconds = Math.max(1, SharedPrefUtils.getDelayBetweenReconnect(this));

        String title = getString(R.string.reconnection_to) + serverInfo;
        String message = getString(R.string.try_number) + attempt;
        updateNotificationWithMessage(title, message);
        serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                .connectionState(ConnectionState.RECONNECTING)
                .exception(new PVNClientException(message))
                .serverInfo(serverInfo)
                .build());

        XLog.tag(TAG).i("Restore: retrying server %d in %ds [attempt %d]", failedServerId, delaySeconds, attempt);
        restoreHandler.postDelayed(() -> {
            if (restoringSession) {
                startConnectionAttempt(failedServerId);
            }
        }, delaySeconds * 1000L);
    }

    private synchronized void unregisterNetworkWaitCallback() {
        if (networkWaitCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkWaitCallback);
            } catch (Exception e) {
                XLog.tag(TAG).w("Failed to unregister network-wait callback: %s", e.getMessage());
            }
            networkWaitCallback = null;
        }
    }

    private void startConnectionAttempt(int initialServerId) {
        submittedConnectionAttempt = executorService.submit(() -> {
            try {
                setConnectionState(ConnectionState.CONNECTING, null);

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(Constants.ERROR_CONNECTED_NOTIFICATION_ID);

                String sniHostname = SharedPrefUtils.getSniHostname(getApplicationContext());
                BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

                SniSpoofingMode sniSpoofingMode = null;
                if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
                    sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(this);
                }

                int serverId = initialServerId;
                if (serverId == START_FROM_TILE_AUTO) {
                    XLog.tag(TAG).i("Connect requested from Quick Settings tile");
                    if (SharedPrefUtils.getResetSelectedServerEnabled(this)) {
                        serverId = SELECTED_SERVER_ID_AUTO;
                    } else {
                        ServerEntity server = getSelectedServer();
                        if (server != null) {
                            XLog.tag(TAG).i("Resuming previously selected server [id=%d, name=%s]", server.getId(), server.getName());
                            serverId = server.getId();
                        } else {
                            XLog.tag(TAG).i("No previously selected server — using auto-select");
                            serverId = SELECTED_SERVER_ID_AUTO;
                        }
                    }
                }

                if (serverId == SELECTED_SERVER_ID_AUTO) {
                    try {
                        XLog.tag(TAG).i("Auto-selecting fastest server via login");
                        updateNotificationWithMessage(getString(R.string.connecting_auto), "");
                        List<ServerEntity> serverEntities = appDatabase.serverDAO().getServerList(false);
                        SpeedTestResult loginResult = SpeedTestUtils.findServerByLogin(serverEntities, sniHostname, bypassCensorshipMethod, sniSpoofingMode);
                        if (loginResult == null || Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        ServerEntity server = loginResult.getServerEntity();
                        if (server == null) {
                            return;
                        }
                        XLog.tag(TAG).i("Auto-selected server [id=%d, name=%s]", server.getId(), server.getName());
                        setSelectedServer(server.getId());
                        connect(server, sniHostname, loginResult.getAccessToken());
                    } catch (PVNClientException e) {
                        // During a reconnect episode a failed scan is not terminal: the network may
                        // have just come back and not be usable yet. Keep recovering — only a cold
                        // user-initiated connect is allowed to fail hard.
                        if (restoringSession) {
                            XLog.tag(TAG).w("Auto-select failed during reconnect: %s — continuing recovery", e.getMessage());
                            if (!NetworkUtils.isOnline(connectivityManager)) {
                                enterWaitingForNetwork(SELECTED_SERVER_ID_AUTO);
                            } else {
                                submittedConnectionAttempt = executorService.submit(this::handleFallbackToAllServers);
                            }
                            return;
                        }
                        XLog.tag(TAG).e("Auto-select failed — all servers unreachable: %s", e.getMessage());
                        disconnect(e);
                    }
                } else {
                    XLog.tag(TAG).i("Connecting to server [id=%d]", serverId);
                    setSelectedServer(serverId);
                    ServerEntity server = getSelectedServer();
                    connect(server, sniHostname, null);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | RuntimeException | UnknownHostException e) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                XLog.tag(TAG).e("Unexpected error during connect setup: %s", e.getMessage());
                disconnect(new PVNClientException(e.getMessage()));
            }
        });
    }

    private void switchState(ConnectionState connectionState, int reconnectCount) {
        switchState(connectionState, reconnectCount, null);
    }

    private void switchState(ConnectionState connectionState, int reconnectCount, String disconnectReason) {
        XLog.tag(TAG).i("State transition -> %s%s", connectionState,
                reconnectCount > 0 ? " [attempt " + reconnectCount + "]" : "");
        switch (connectionState) {
            case DISCONNECTED -> {
                if (activeConnection.get() != null) {
                    disconnect(null, disconnectReason);
                }
            }
            case CONNECTING -> setConnectionState(ConnectionState.CONNECTING, null);
            case CONNECTED -> {
                // Success fully closes the recovery episode: clear the restore flag, zero the
                // attempt counters, refill the budget and drop any pending restore work.
                restoringSession = false;
                restoreRetryCount.set(0);
                remainingFallbackBudget.set(SharedPrefUtils.getReconnectAttemptsCount(this));
                restoreHandler.removeCallbacksAndMessages(null);
                String serverInfo = getActionConnectServerInfo();
                updateNotificationWithMessage(getString(R.string.connected_to) + serverInfo, "");

                serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                        .connectionState(ConnectionState.CONNECTED)
                        .serverInfo(serverInfo)
                        .build());
            }
            case RECONNECTING -> {
                String title = getString(R.string.reconnection_to) + getActionConnectServerInfo();
                String errorMessage = getString(R.string.try_number) + reconnectCount;
                updateNotificationWithMessage(title, errorMessage);

                setConnectionState(ConnectionState.RECONNECTING, new PVNClientException(errorMessage));
            }
        }
    }

    @NonNull
    public String getActionConnectServerInfo() {
        return Optional.ofNullable(activeConnection.get())
                .map(FptnConnection::getServerEntity)
                .map(ServerEntity::getServerInfo)
                .orElse("");
    }

    private void setConnectionState(ConnectionState connectionState, PVNClientException exception) {
        setConnectionState(connectionState, exception, null);
    }

    private void setConnectionState(ConnectionState connectionState, PVNClientException exception, String disconnectReason) {
        serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                .connectionState(connectionState)
                .exception(exception)
                .disconnectReason(disconnectReason)
                .build());
    }

    private void connect(ServerEntity serverEntity, String sniHostname, String preFetchedToken) throws UnknownHostException {
        // A cold connect opens a new episode with the full configured budget. During a restore
        // episode the remaining budget carries over, so the configured attempts count is honoured
        // across same-server retries, scans and handoffs. <= 0 means uninitialised (fresh process
        // restarted by the OS mid-episode) — start a full episode then.
        if (!restoringSession || remainingFallbackBudget.get() <= 0) {
            remainingFallbackBudget.set(SharedPrefUtils.getReconnectAttemptsCount(this));
        }
        int budget = remainingFallbackBudget.get();
        boolean autoFallbackEnabled = SharedPrefUtils.getAutoFallbackEnabled(this);
        int fallbackThreshold = (autoFallbackEnabled && !serverEntity.IsAuto())
                ? Math.min(SharedPrefUtils.getAutoFallbackThreshold(this), budget) : 0;
        connectInternal(serverEntity, sniHostname, preFetchedToken, budget, fallbackThreshold);
    }

    private void connectWithRemainingAttempts(ServerEntity serverEntity, String sniHostname, String preFetchedToken) throws UnknownHostException {
        // Handoff after a scan: continue the episode on the REMAINING budget. Resetting it here
        // (the old behaviour) made the drop→scan→handoff cycle unbounded for finite attempt
        // counts — the budget refilled to max on every handoff.
        int budget = Math.max(1, remainingFallbackBudget.get());
        boolean autoFallbackEnabled = SharedPrefUtils.getAutoFallbackEnabled(this);
        int fallbackThreshold = (autoFallbackEnabled && !serverEntity.IsAuto())
                ? Math.min(SharedPrefUtils.getAutoFallbackThreshold(this), budget) : 0;
        connectInternal(serverEntity, sniHostname, preFetchedToken, budget, fallbackThreshold);
    }

    private void connectInternal(ServerEntity serverEntity, String sniHostname, String preFetchedToken, int maxReconnectCount, int fallbackThreshold) throws UnknownHostException {
        XLog.tag(TAG).i("Connecting to [%s] at %s:%d via sni=[%s]",
                serverEntity.getServerInfo(), serverEntity.getHost(), serverEntity.getPort(), sniHostname);
        updateNotificationWithMessage(getString(R.string.connecting_to) + serverEntity.getServerInfo(), "");

        acquirePowerLock();

        NetworkType networkType = NetworkType.UNKNOWN;
        String currentIPAddress = NetworkUtils.UNKNOWN_IP;

        boolean reconnectOnChangeIPEnabled = SharedPrefUtils.getReconnectOnChangeIPEnabled(this);
        boolean reconnectOnChangeNetworkTypeEnabled = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this);
        if (reconnectOnChangeIPEnabled || reconnectOnChangeNetworkTypeEnabled) {
            registerNetworkCallback();

            if (reconnectOnChangeNetworkTypeEnabled) {
                networkType = NetworkUtils.getActiveNetworkType(connectivityManager);
            }

            if (reconnectOnChangeIPEnabled) {
                currentIPAddress = NetworkUtils.getCurrentIPAddress();
            }
        } else {
            XLog.tag(TAG).d("Network change reconnect disabled in settings");
        }
        int delayBetweenAttempts = SharedPrefUtils.getDelayBetweenReconnect(this);

        FptnConnection connection;

        BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

        SniSpoofingMode sniSpoofingMode = null;
        if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
            sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(this);
        }
        XLog.tag(TAG).i("Connection params [bypass=%s, spoofingMode=%s, maxRetries=%d, fallbackThreshold=%d, retryDelay=%ds, watchIP=%b, watchNetwork=%b]",
                bypassCensorshipMethod,
                sniSpoofingMode,
                maxReconnectCount, fallbackThreshold, delayBetweenAttempts,
                reconnectOnChangeIPEnabled, reconnectOnChangeNetworkTypeEnabled);

        PerAppVpnMode perAppVpnMode = SharedPrefUtils.getPerAppVPNMode(this);
        List<AppInfo> appInfos = new ArrayList<>();
        if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED || perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED) {
            List<AppInfo> packages = appDatabase.appInfoDAO().getAll().stream()
                    .filter(appInfoEntity -> {
                        if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
                            return appInfoEntity.isAllowed();
                        } else {
                            return appInfoEntity.isDisallowed();
                        }
                    }).map(
                            appInfo -> AppInfo.builder()
                                    .packageName(appInfo.getPackageName()).build()
                    ).collect(Collectors.toList());
            appInfos.addAll(packages);
        }

        List<String> allServerHosts = appDatabase.serverDAO().getServerList(false)
                .stream()
                .map(ServerEntity::getHost)
                .collect(Collectors.toList());

        boolean adBlockEnabled = SharedPrefUtils.getAdBlockEnabled(this);
        boolean domainBlacklistEnabled = SharedPrefUtils.getDomainBlacklistEnabled(this);
        String domainBlacklist = domainBlacklistEnabled ? SharedPrefUtils.getDomainBlacklistDomains(this) : null;
        DomainBlocker domainBlocker = (adBlockEnabled || domainBlacklistEnabled)
                ? new DomainBlocker(this, adBlockEnabled, domainBlacklist) : null;

        boolean customDnsEnabled = SharedPrefUtils.getCustomDnsEnabled(this);
        String customDnsIpv4 = customDnsEnabled ? SharedPrefUtils.getCustomDnsIpv4(this) : null;

        DnsServers preFetchedDnsServers = (cachedDnsServers != null && cachedDnsServersServerId == serverEntity.getId())
                ? cachedDnsServers : null;

        connection = new FptnConnection(
                this,
                nextConnectionId.getAndIncrement(),
                serverEntity,
                allServerHosts,
                currentIPAddress,
                networkType,
                maxReconnectCount,
                fallbackThreshold,
                delayBetweenAttempts,
                sniHostname,
                bypassCensorshipMethod,
                sniSpoofingMode,
                perAppVpnMode,
                appInfos,
                preFetchedToken,
                connectivityManager,
                domainBlocker,
                customDnsIpv4,
                preFetchedDnsServers
        );
        connection.setConfigureVpnIntent(launchMainActivityPendingIntent);
        connection.start();

        setActiveConnection(connection);
    }

    private void handleFallbackToAllServers() {
        XLog.tag(TAG).i("Fallback: initiating all-server scan after repeated failures");
        try {
            String lastServerInfo = getActionConnectServerInfo();
            setActiveConnection(null);

            String sniHostname = SharedPrefUtils.getSniHostname(getApplicationContext());
            BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);
            SniSpoofingMode sniSpoofingMode = null;
            if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
                sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(this);
            }
            int fallbackThreshold = SharedPrefUtils.getAutoFallbackThreshold(this);
            int delayBetweenAttempts = SharedPrefUtils.getDelayBetweenReconnect(this);

            // Deduct the cost of reconnects that triggered this fallback (once)
            int remainingBudget = remainingFallbackBudget.addAndGet(-fallbackThreshold);
            if (remainingBudget <= 0) {
                XLog.tag(TAG).e("Fallback: reconnection budget exhausted");
                disconnect(new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE));
                return;
            }

            int maxReconnectCount = SharedPrefUtils.getReconnectAttemptsCount(this);
            while (!Thread.currentThread().isInterrupted()) {
                // Offline: scanning 52 servers every second is pointless and drains the battery
                // (inviting an OEM process kill). Park in WAITING_FOR_NETWORK and resume via the
                // network callback instead.
                if (!NetworkUtils.isOnline(connectivityManager)) {
                    XLog.tag(TAG).i("Fallback: network offline — suspending scan, waiting for network");
                    enterWaitingForNetwork(SELECTED_SERVER_ID_AUTO);
                    return;
                }
                int currentAttempt = maxReconnectCount - remainingFallbackBudget.get() + 1;
                String errorMessage = getString(R.string.try_number_fallback) + currentAttempt;
                updateNotificationWithMessage(getString(R.string.connecting_auto), errorMessage);
                serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                        .connectionState(ConnectionState.RECONNECTING)
                        .exception(new PVNClientException(errorMessage))
                        .serverInfo(getString(R.string.connecting_auto))
                        .build());

                List<ServerEntity> serverEntities = appDatabase.serverDAO().getServerList(false);
                try {
                    SpeedTestResult loginResult = SpeedTestUtils.findServerByLogin(serverEntities, sniHostname, bypassCensorshipMethod, sniSpoofingMode);
                    if (loginResult == null || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    ServerEntity server = loginResult.getServerEntity();
                    if (server == null) {
                        XLog.tag(TAG).e("Fallback: all-server scan found no reachable server");
                        disconnect(new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE));
                        return;
                    }
                    XLog.tag(TAG).i("Fallback: selected [id=%d, name=%s] with %d scan budget remaining",
                            server.getId(), server.getName(), remainingFallbackBudget.get());
                    setSelectedServer(server.getId());
                    // Handoff from scan to a full connection attempt. The login probe passed, but
                    // the full establish (TUN + DNS + WebSocket) can still fail. Keep the session
                    // in restore mode so that failure routes back into retry/fallback instead of a
                    // terminal disconnect — otherwise a single handoff miss kills the service even
                    // with unlimited reconnect attempts configured.
                    restoringSession = true;
                    restoreRetryCount.set(0);
                    connectWithRemainingAttempts(server, sniHostname, loginResult.getAccessToken());
                    return;
                } catch (PVNClientException scanException) {
                    int remaining = remainingFallbackBudget.addAndGet(-1);
                    XLog.tag(TAG).e("Fallback: scan failed [%s], remaining budget=%d — retrying after %ds",
                            scanException.getMessage(), remaining, delayBetweenAttempts);
                    if (remaining <= 0) {
                        XLog.tag(TAG).e("Fallback: reconnection budget exhausted");
                        disconnect(new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE));
                        return;
                    }
                    Thread.sleep((long) delayBetweenAttempts * 1000L);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | RuntimeException | UnknownHostException e) {
            XLog.tag(TAG).e("Fallback: unexpected error: %s", e.getMessage());
            disconnect(new PVNClientException(e.getMessage()));
        }
    }

    private synchronized void acquirePowerLock() {
        // release previous power lock
        releasePowerLock();
        // we need this lock so our service gets not affected by Doze Mode
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FPTN_SERVICE_POWER_LOCK);
            wakeLock.acquire(5000);
        } catch (Exception e) {
            XLog.tag(TAG).e("Can't acquire power lock!", e);
        }
    }

    private synchronized void releasePowerLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                XLog.tag(TAG).e("Can't release power lock!", e);
            }
        }
    }

    private void notifyTileListeningState() {
        TileService.requestListeningState(this, new ComponentName(this, FptnTileService.class));
        // Delayed retry: the system may rate-limit or deduplicate rapid requestListeningState()
        // calls, causing a DISCONNECTED notification to be silently dropped. A second attempt
        // 500 ms later recovers from this without interfering with a new connection — the check
        // guards against calling when the user has already reconnected.
        final Context appCtx = getApplicationContext();
        final ComponentName tileName = new ComponentName(this, FptnTileService.class);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FptnTileService.getServiceStateMutableLiveData().getValue() == ConnectionState.DISCONNECTED) {
                TileService.requestListeningState(appCtx, tileName);
            }
        }, 500);
    }

    private void setActiveConnection(FptnConnection connection) {
        FptnConnection oldConnection = activeConnection.getAndSet(connection);
        if (oldConnection != null) {
            XLog.tag(TAG).i("Tearing down previous connection [id=%d]", oldConnection.getConnectionId());
            oldConnection.shutdown();
        }
    }

    private void disconnect() {
        disconnect(null, null);
    }

    private void disconnect(PVNClientException exception) {
        disconnect(exception, null);
    }

    private String resolveDisconnectReason(String key) {
        if (key == null) return null;
        switch (key) {
            case DISCONNECT_REASON_SYSTEM_REVOKED:    return getString(R.string.disconnect_reason_system_revoked);
            case DISCONNECT_REASON_CLOSED_UNEXPECTEDLY: return getString(R.string.disconnect_reason_closed_unexpectedly);
            case DISCONNECT_REASON_UNEXPECTED_ERROR:  return getString(R.string.disconnect_reason_unexpected_error);
            default: return key;
        }
    }

    private void disconnect(PVNClientException exception, String disconnectReasonKey) {
        // Cancel any pending restore work so a scheduled retry can't resurrect a torn-down session.
        restoringSession = false;
        restoreRetryCount.set(0);
        restoreHandler.removeCallbacksAndMessages(null);
        // Stop an in-flight scan/connect task too — otherwise it can outlive this disconnect
        // and re-establish a session nobody asked for. Self-cancel (when disconnect runs inside
        // that very task) is harmless: nothing below blocks on the interrupt flag.
        Future<?> pendingAttempt = submittedConnectionAttempt;
        if (pendingAttempt != null && !pendingAttempt.isDone()) {
            pendingAttempt.cancel(true);
        }
        String disconnectReason = resolveDisconnectReason(disconnectReasonKey);
        if (exception == null && disconnectReason == null) {
            XLog.tag(TAG).w("DISCONNECT REASON: user action");
        } else if (disconnectReason != null) {
            XLog.tag(TAG).w("DISCONNECT REASON: %s", disconnectReason);
        } else {
            XLog.tag(TAG).w("DISCONNECT REASON: error [code=%s, message=%s]",
                    exception.errorCode, exception.errorMessage);
        }
        // stop and null existed connection
        setActiveConnection(null);
        // remove service from foreground - and remove notification
        stopForeground(STOP_FOREGROUND_REMOVE);
        // sometimes need to remove notification explicitly
        removeForegroundNotification();
        //send to UI activity that state is disconnected.
        setConnectionState(ConnectionState.DISCONNECTED, exception, disconnectReason);

        if (exception != null) {
            ErrorCode errorCode = exception.errorCode;
            if (errorCode != ErrorCode.UNKNOWN_ERROR) {
                String stringResourceByName = getStringResourceByName(getApplication(), errorCode.getValue());
                showErrorNotification(stringResourceByName);
            } else {
                showErrorNotification(exception.errorMessage);
            }
        }

        // Release wakelock
        releasePowerLock();

        // unregister network callbacks
        unregisterNetworkWaitCallback();
        unregisterNetworkCallback();

        if (SharedPrefUtils.getResetSelectedServerEnabled(this)
                || (SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(this) && exception != null)) {
            executorService.submit(() -> {
                try {
                    resetSelectedServer();

                    //send to UI activity that state is disconnected.
                    setConnectionState(ConnectionState.DISCONNECTED, exception, disconnectReason);
                } catch (ExecutionException | InterruptedException e) {
                    XLog.tag(TAG).e("Failed to reset selected server: %s", e.getMessage());
                }
            });
        }

        // stop service
        stopSelf();
    }

    private void removeForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.MAIN_CONNECTED_NOTIFICATION_ID);
    }

    private void startForegroundWithNotification(String title) {
        Notification notification = createNotification(title, "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
        }
    }

    private void updateNotificationWithMessage(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, message);
        notificationManager.notify(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.disconnect_action), disconnectPendingIntent)
                .build();
        Notification.Builder builder = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
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

    private void showReconnectionFailedNotification() {
        Notification notification = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(getApplication().getString(R.string.reconnecting_failed))
                .setContentIntent(launchMainActivityPendingIntent)
                .setAutoCancel(true) // if you tap on notification - opens activity and notification dismissed
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.INFO_NOTIFICATION_NOTIFICATION_ID, notification);
    }

    private void showErrorNotification(String message) {
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.reconnect_action), reconnectPendingIntent)
                .build();
        Notification notification = new Notification.Builder(this, Constants.ERROR_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(getApplication().getString(R.string.error))
                .setContentText(message)
                .setAutoCancel(true) // if you tap on notification - opens activity and notification dismissed
                .setContentIntent(launchMainActivityPendingIntent)
                .addAction(actionDisconnect)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.ERROR_CONNECTED_NOTIFICATION_ID, notification);
    }
}
