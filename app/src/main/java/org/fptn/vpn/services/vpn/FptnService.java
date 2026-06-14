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
import android.os.IBinder;
import android.os.PowerManager;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.NetworkType;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.tile.FptnTileService;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import kotlin.Triple;
import lombok.Getter;

@SuppressLint("VpnServicePolicy")
public class FptnService extends VpnService {
    private static final String TAG = FptnService.class.getSimpleName();

    public static final String ACTION_CONNECT = "FptnService:CONNECT";
    public static final String ACTION_DISCONNECT = "FptnService:DISCONNECT";
    public static final String ACTION_BIND = "FptnService:BIND";
    public static final String FPTN_SERVICE_POWER_LOCK = "FptnService::POWER_LOCK";

    private final AtomicReference<FptnConnection> activeConnection = new AtomicReference<>();
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    // Pending Intent to reconnect from notification
    private PendingIntent reconnectPendingIntent;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Getter
    private final MutableLiveData<FptnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(FptnServiceState.INITIAL);
    @Getter
    private final MutableLiveData<Triple<String, String, Long>> speedAndDurationMutableLiveData = new MutableLiveData<>();

    @Getter
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    private Observer<FptnServiceState> serviceStateObserver;

    private AppDatabase appDatabase;

    /**
     * LocalBinder - just the way to give HomeActivity link on FptnService object
     */
    private final IBinder binder = new LocalBinder();
    private Future<?> submittedConnectionAttempt;

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


    public void updateSpeedInfo(String downloadSpeed, String uploadSpeed, long duration) {
        if (serviceStateMutableLiveData.getValue().getConnectionState() == ConnectionState.CONNECTED) {
            updateNotificationWithMessage(
                    String.format("%s %s", getString(R.string.connected_to), getActionConnectServerInfo()),
                    String.format(getString(R.string.download_upload_speed_pattern), downloadSpeed, uploadSpeed)
            );

            speedAndDurationMutableLiveData.postValue(new Triple<>(downloadSpeed, uploadSpeed, duration));
        }
    }

    public void sendExceptionToService(PVNClientException exception) {
        disconnect(exception);
        if (Objects.equals(exception.errorCode, ErrorCode.RECONNECTING_FAILED)) {
            showReconnectionFailedNotification();
        }
    }

    public void updateConnectionState(ConnectionState connectionState, int reconnectionCount) {
        switchState(connectionState, reconnectionCount);
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
        XLog.tag(TAG).i("Service created");

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
            FptnTileService.getServiceStateMutableLiveData().postValue(fptnServiceState.getConnectionState());
            // to initialize call onStartListening() in FptnTileService
            TileService.requestListeningState(this, new ComponentName(this, FptnTileService.class));
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
        //send initial value
        FptnTileService.getServiceStateMutableLiveData().postValue(serviceStateMutableLiveData.getValue().getConnectionState());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if service crashed previously
        if (intent == null) {
            XLog.tag(TAG).e("Received null intent — service likely restarted after crash; stopping");
            stopSelf();
            return START_NOT_STICKY;
        }

        ConnectionState currentState = serviceStateMutableLiveData.getValue().getConnectionState();
        boolean isActiveState = currentState.isActiveState();
        XLog.tag(TAG).i("Received command [action=%s, state=%s]", intent.getAction(), currentState);

        if (ACTION_CONNECT.equals(intent.getAction()) && !isActiveState) {
            startForegroundWithNotification(getString(R.string.connecting));

            if (!NetworkUtils.isOnline(connectivityManager)) {
                XLog.tag(TAG).e("No internet connection — aborting connect");
                disconnect(new PVNClientException(ErrorCode.NO_ACTIVE_INTERNET_CONNECTIONS));
                return START_NOT_STICKY;
            }

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

                    int serverId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);

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
                            ServerEntity server = loginResult.getServerEntity();
                            if (server == null && Thread.currentThread().isInterrupted()) {
                                // Must never happen - just to process interruption
                                return;
                            }
                            XLog.tag(TAG).i("Auto-selected server [id=%d, name=%s]", server.getId(), server.getName());
                            setSelectedServer(server.getId());
                            connect(server, sniHostname, loginResult.getAccessToken());
                        } catch (PVNClientException e) {
                            XLog.tag(TAG).e("Auto-select failed — all servers unreachable: %s", e.getMessage());
                            disconnect(e);
                        }
                    } else {
                        XLog.tag(TAG).i("Connecting to server [id=%d]", serverId);
                        setSelectedServer(serverId);
                        ServerEntity server = getSelectedServer();
                        connect(server, sniHostname, null);
                    }
                } catch (ExecutionException | InterruptedException | RuntimeException |
                         UnknownHostException e) {
                    XLog.tag(TAG).e("Unexpected error during connect setup: %s", e.getMessage());
                    disconnect(new PVNClientException(e.getMessage()));
                }
            });

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

        // if it stops - it stops
        return START_NOT_STICKY;
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
    public void onDestroy() {
        XLog.tag(TAG).i("Service destroyed [state=%s]", serviceStateMutableLiveData.getValue().getConnectionState());

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
                if (currentConnection != null && (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.RECONNECTING)) {
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

    private void switchState(ConnectionState connectionState, int reconnectCount) {
        XLog.tag(TAG).i("State transition -> %s%s", connectionState,
                reconnectCount > 0 ? " [attempt " + reconnectCount + "]" : "");
        switch (connectionState) {
            case DISCONNECTED -> {
                if (activeConnection.get() != null) {
                    disconnect();
                }
            }
            case CONNECTING -> setConnectionState(ConnectionState.CONNECTING, null);
            case CONNECTED -> {
                String title_connected_to = getString(R.string.connected_to) + getActionConnectServerInfo();
                updateNotificationWithMessage(title_connected_to, "");

                setConnectionState(ConnectionState.CONNECTED, null);
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
        serviceStateMutableLiveData.postValue(FptnServiceState.builder()
                .connectionState(connectionState)
                .exception(exception)
                .build());
    }

    private void connect(ServerEntity serverEntity, String sniHostname, String preFetchedToken) throws UnknownHostException {
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
        int maxReconnectCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        int delayBetweenAttempts = SharedPrefUtils.getDelayBetweenReconnect(this);

        FptnConnection connection;

        BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

        SniSpoofingMode sniSpoofingMode = null;
        if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
            sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(this);
        }
        XLog.tag(TAG).i("Connection params [bypass=%s, spoofingMode=%s, maxRetries=%d, retryDelay=%ds, watchIP=%b, watchNetwork=%b]",
                bypassCensorshipMethod,
                sniSpoofingMode,
                maxReconnectCount, delayBetweenAttempts,
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

        connection = new FptnConnection(
                this,
                nextConnectionId.getAndIncrement(),
                serverEntity,
                currentIPAddress,
                networkType,
                maxReconnectCount,
                delayBetweenAttempts,
                sniHostname,
                bypassCensorshipMethod,
                sniSpoofingMode,
                perAppVpnMode,
                appInfos,
                preFetchedToken
        );
        connection.setConfigureVpnIntent(launchMainActivityPendingIntent);
        connection.start();

        setActiveConnection(connection);
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

    private void setActiveConnection(FptnConnection connection) {
        FptnConnection oldConnection = activeConnection.getAndSet(connection);
        if (oldConnection != null) {
            XLog.tag(TAG).i("Tearing down previous connection [id=%d]", oldConnection.getConnectionId());
            oldConnection.shutdown();
        }
    }

    private void disconnect() {
        //disconnect without exception
        disconnect(null);
    }

    private void disconnect(PVNClientException exception) {
        if (exception == null) {
            XLog.tag(TAG).i("Disconnecting [reason=user]");
        } else {
            XLog.tag(TAG).w("Disconnecting [reason=error, code=%s, message=%s]",
                    exception.errorCode, exception.errorMessage);
        }
        // stop and null existed connection
        setActiveConnection(null);
        // remove service from foreground - and remove notification
        stopForeground(STOP_FOREGROUND_REMOVE);
        // sometimes need to remove notification explicitly
        removeForegroundNotification();
        //send to UI activity that state is disconnected.
        setConnectionState(ConnectionState.DISCONNECTED, exception);

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

        // unregister network callback
        unregisterNetworkCallback();

        if (SharedPrefUtils.getResetSelectedServerEnabled(this)
                || (SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(this) && exception != null)) {
            executorService.submit(() -> {
                try {
                    resetSelectedServer();

                    //send to UI activity that state is disconnected.
                    setConnectionState(ConnectionState.DISCONNECTED, exception);
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
