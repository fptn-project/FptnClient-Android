package org.fptn.vpn.services;

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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.NetworkType;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.NetworkUtils;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.HomeActivity;
import org.fptn.vpn.views.speedtest.SpeedTestUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Triple;
import lombok.Getter;

public class CustomVpnService extends VpnService {
    private static final String TAG = CustomVpnService.class.getSimpleName();

    public static final String ACTION_CONNECT = "CustomVpnService:CONNECT";
    public static final String ACTION_DISCONNECT = "CustomVpnService:DISCONNECT";
    public static final String ACTION_BIND = "CustomVpnService:BIND";
    public static final String FPTN_SERVICE_POWER_LOCK = "CustomVpnService::POWER_LOCK";

    private final AtomicReference<CustomVpnConnection> activeConnection = new AtomicReference<>();

    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    // Pending Intent to reconnect from notification
    private PendingIntent reconnectPendingIntent;

    private FptnServerRepository fptnServerRepository;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Getter
    private final MutableLiveData<CustomVpnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(CustomVpnServiceState.INITIAL);
    @Getter
    private final MutableLiveData<Triple<String, String, Long>> speedAndDurationMutableLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    private Observer<CustomVpnServiceState> serviceStateObserver;

    /**
     * LocalBinder - just the way to give HomeActivity link on CustomVpnService object
     */
    private final IBinder binder = new LocalBinder();

    public static void bindService(Context context, ServiceConnection connection) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_BIND);
        context.bindService(intent, connection, BIND_AUTO_CREATE);
    }

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

    public class LocalBinder extends Binder {
        public CustomVpnService getService() {
            return CustomVpnService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /* Static methods to start/stop service */
    public synchronized static void startToConnect(Context context, FptnServerDto fptnServerDto) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_CONNECT);
        if (fptnServerDto != null) {
            intent.putExtra(SELECTED_SERVER, fptnServerDto.id);
        }
        context.startService(intent);
    }

    public synchronized static void startToConnect(Context context) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_CONNECT);
        // Now it method called only from FptnTileService
        intent.putExtra(SELECTED_SERVER, START_FROM_TILE_AUTO);
        context.startService(intent);
    }

    public synchronized static void startToDisconnect(Context context) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_DISCONNECT);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "CustomVpnService.onCreate() Thread.Id: " + Thread.currentThread().getId());

        // pending intent for open MainActivity on tap
        launchMainActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        // pending intent for disconnect button in connected notification
        disconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, CustomVpnService.class)
                        .setAction(CustomVpnService.ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE);

        // pending intent for reconnect button in error notification
        reconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, CustomVpnService.class)
                        .setAction(CustomVpnService.ACTION_CONNECT),
                PendingIntent.FLAG_IMMUTABLE);

        // create fptnServerRepository
        fptnServerRepository = new FptnServerRepository(getApplicationContext());

        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        serviceStateObserver = (customVpnServiceState) -> {
            FptnTileService.getServiceStateMutableLiveData().postValue(customVpnServiceState.getConnectionState());
            // to initialize call onStartListening() in FptnTileService
            TileService.requestListeningState(this, new ComponentName(this, FptnTileService.class));
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
        //send initial value
        FptnTileService.getServiceStateMutableLiveData().postValue(serviceStateMutableLiveData.getValue().getConnectionState());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand() Intent: " + intent + ", Thread.Id: " + Thread.currentThread().getId());

        executorService.submit(() -> {
            try {
                /* check is internet connection available */
                if (!NetworkUtils.isOnline(connectivityManager)) {
                    Log.e(TAG, "onStartCommand: no active internet connections!");
                    disconnect(new PVNClientException(ErrorCode.NO_ACTIVE_INTERNET_CONNECTIONS));
                    return;
                }

                // dismiss error notification
                NotificationManager notificationManager = (NotificationManager) getSystemService(
                        NOTIFICATION_SERVICE);
                notificationManager.cancel(Constants.ERROR_CONNECTED_NOTIFICATION_ID);

                String sniHostname = SharedPrefUtils.getSniHostname(getApplicationContext());
                BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

                boolean isActiveState = serviceStateMutableLiveData.getValue().getConnectionState().isActiveState();
                if (ACTION_DISCONNECT.equals(intent.getAction()) && isActiveState) {
                    if (SharedPrefUtils.getResetSelectedServerEnabled(this)) {
                        fptnServerRepository.resetSelected().get();
                    }
                    // stop running threads
                    disconnect();
                } else if (ACTION_CONNECT.equals(intent.getAction()) && !isActiveState) {
                    setConnectionState(ConnectionState.CONNECTING, null);

                    int serverId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);

                    // Process startService from TileService
                    if (serverId == START_FROM_TILE_AUTO) {
                        Log.i(TAG, "onStartCommand: start from tileService");
                        // If reset selected server enabled - process like average auto
                        if (SharedPrefUtils.getResetSelectedServerEnabled(this)) {
                            Log.i(TAG, "onStartCommand: start from tileService. Reset selected server enabled");
                            serverId = SELECTED_SERVER_ID_AUTO;
                        } else {
                            // But if reset disabled, try to get previously selected server from DB
                            FptnServerDto server = fptnServerRepository.getSelected().get();
                            if (server != null) {
                                // if found
                                Log.i(TAG, "onStartCommand: start from tileService. Previously selected server found: " + server);
                                serverId = server.id;
                            } else {
                                // if not found - auto
                                Log.i(TAG, "onStartCommand: start from tileService. Previously selected server not found: select auto");
                                serverId = SELECTED_SERVER_ID_AUTO;
                            }
                        }
                    }

                    if (serverId == SELECTED_SERVER_ID_AUTO) {
                        try {
                            List<FptnServerDto> fptnServerDtos = fptnServerRepository.getServersListFuture(false).get();
                            FptnServerDto server = SpeedTestUtils.findFastestServer(fptnServerDtos, sniHostname, bypassCensorshipMethod);
                            fptnServerRepository.setIsSelected(server.id).get();
                            connect(server, sniHostname);
                        } catch (PVNClientException e) {
                            /* We don't need to connect if all servers are unreachable */
                            Log.e(TAG, "onStartCommand: findFastestServer error! ", e);
                            disconnect(e);
                        }
                    } else {
                        Log.i(TAG, "onStartCommand: connectToServer with id: " + serverId);
                        fptnServerRepository.setIsSelected(serverId).get();
                        FptnServerDto server = fptnServerRepository.getSelected().get();
                        connect(server, sniHostname);
                    }
                }
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                disconnect(new PVNClientException(e.getMessage()));
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");

        disconnect();

        if (serviceStateObserver != null) {
            serviceStateMutableLiveData.removeObserver(serviceStateObserver);
            serviceStateObserver = null;
        }
    }


    private void registerNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);

                CustomVpnConnection currentConnection = activeConnection.get();
                ConnectionState connectionState = Optional.ofNullable(serviceStateMutableLiveData.getValue())
                        .map(CustomVpnServiceState::getConnectionState).orElse(null);
                if (currentConnection != null && connectionState == ConnectionState.CONNECTED) {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    NetworkCapabilities activeNetworkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (activeNetworkCapabilities != null && activeNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        boolean needReconnectByNetworkType = false;
                        boolean reconnectOnChangeNetworkTypeEnabled = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(CustomVpnService.this);
                        if (reconnectOnChangeNetworkTypeEnabled) {
                            NetworkType activeNetworkType = NetworkUtils.getNetworkType(activeNetworkCapabilities);
                            Log.d(TAG, "ActiveNetworkType: " + activeNetworkType);
                            Log.d(TAG, "ConnectionNetworkType: " + currentConnection.getCurrentNetworkType());
                            needReconnectByNetworkType = activeNetworkType != currentConnection.getCurrentNetworkType();
                            Log.d(TAG, "After check networkType: " + needReconnectByNetworkType);
                            if (needReconnectByNetworkType) {
                                currentConnection.setCurrentNetworkType(activeNetworkType);
                            }
                        }

                        boolean needReconnectByIp = false;
                        boolean reconnectOnChangeIPEnabled = SharedPrefUtils.getReconnectOnChangeIPEnabled(CustomVpnService.this);
                        if (reconnectOnChangeIPEnabled) {
                            String currentIPAddress = NetworkUtils.getCurrentIPAddress();
                            needReconnectByIp = !Objects.equals(currentIPAddress, currentConnection.getCurrentIPAddress());
                            Log.d(TAG, "After check actual IP: " + needReconnectByIp);
                            if (needReconnectByIp) {
                                currentConnection.setCurrentIPAddress(currentIPAddress);
                            }
                        }

                        if (needReconnectByNetworkType || needReconnectByIp) {
                            Log.d(TAG, "Reconnect initialized");
                            currentConnection.onConnectionFailure();
                        }
                    }
                }
            }
        };

        connectivityManager.registerNetworkCallback(NetworkUtils.createNetworkRequest(), networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    private void switchState(ConnectionState connectionState, int reconnectCount) {
        switch (connectionState) {
            case DISCONNECTED -> disconnect();
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
    private String getActionConnectServerInfo() {
        return Optional.ofNullable(activeConnection.get())
                .map(CustomVpnConnection::getFptnServerDto)
                .map(FptnServerDto::getServerInfo)
                .orElse("");
    }

    private void setConnectionState(ConnectionState connectionState, PVNClientException
            exception) {
        serviceStateMutableLiveData.postValue(CustomVpnServiceState.builder()
                .connectionState(connectionState)
                .exception(exception)
                .build());
    }

    private void connect(FptnServerDto fptnServerDto, String sniHostname) {
        // Moving VPNService to foreground to give it higher priority in system
        startForegroundWithNotification(getString(R.string.connecting_to) + fptnServerDto.getServerInfo());

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
            Log.d(TAG, "Reconnect on change network type and IP disabled in settings");
        }
        int maxReconnectCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        int delayBetweenAttempts = SharedPrefUtils.getDelayBetweenReconnect(this);

        CustomVpnConnection connection;

        BypassCensorshipMethod bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

        connection = new CustomVpnConnection(
                this,
                nextConnectionId.getAndIncrement(),
                fptnServerDto,
                currentIPAddress,
                networkType,
                maxReconnectCount,
                delayBetweenAttempts,
                sniHostname,
                bypassCensorshipMethod
        );
        connection.setConfigureVpnIntent(launchMainActivityPendingIntent);
        connection.start();

        setActiveConnection(connection);
    }

    @SuppressLint("WakelockTimeout")
    private void acquirePowerLock() {
        // release previous power lock
        releasePowerLock();
        // we need this lock so our service gets not affected by Doze Mode
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    FPTN_SERVICE_POWER_LOCK);
            wakeLock.acquire();
        } catch (Exception e) {
            Log.e(TAG, "Can't acquire power lock!", e);
        }
    }

    private void releasePowerLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                Log.e(TAG, "Can't release power lock!", e);
            }
        }
    }

    private void setActiveConnection(CustomVpnConnection connection) {
        CustomVpnConnection oldConnection = activeConnection.getAndSet(connection);
        if (oldConnection != null) {
            oldConnection.shutdown();
        }
    }

    private void disconnect() {
        //disconnect without exception
        disconnect(null);
    }

    private void disconnect(PVNClientException exception) {
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
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // unregister network callback
        unregisterNetworkCallback();

        // stop service
        stopSelf();
    }

    private void removeForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.MAIN_CONNECTED_NOTIFICATION_ID);
    }

    private void startForegroundWithNotification(String title) {
        NotificationUtils.configureNotificationChannel(this);
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
