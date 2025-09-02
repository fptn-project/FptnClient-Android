package org.fptn.vpn.services;

import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER;
import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER_ID_AUTO;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.HandlerMessageTypes;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.utils.NetworkType;
import org.fptn.vpn.utils.NetworkUtils;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.utils.ServiceUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.HomeActivity;
import org.fptn.vpn.views.speedtest.SpeedTestUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.Arrays;
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

public class CustomVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = CustomVpnService.class.getSimpleName();

    public static final String ACTION_CONNECT = "CustomVpnService:CONNECT";
    public static final String ACTION_DISCONNECT = "CustomVpnService:DISCONNECT";
    public static final String ACTION_BIND = "CustomVpnService:BIND";

    public static final String FPTN_SERVICE_POWER_LOCK = "CustomVpnService::POWER_LOCK";

    // for set alarming
    public static final String ACTION_RESET_ALARM = "CustomVpnService:ACTION_RESET_ALARM";
    public static final int ONE_MINUTE = 60_000;

    //Handler - queue of events from another threads, that need to process in this thread
    @Getter
    private Handler handler;

    private final AtomicReference<CustomVpnConnection> activeConnection = new AtomicReference<>();

    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    private FptnServerRepository fptnServerRepository;

    private boolean isNotificationAllowed = false;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Getter
    private final MutableLiveData<CustomVpnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(CustomVpnServiceState.INITIAL);
    @Getter
    private final MutableLiveData<Triple<String, String, Long>> speedAndDurationMutableLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    /**
     * LocalBinder - just the way to give HomeActivity link on CustomVpnService object
     */
    private final IBinder binder = new LocalBinder();

    public static void bindService(Context context, ServiceConnection connection) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_BIND);
        context.bindService(intent, connection, BIND_AUTO_CREATE);
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
    public static void startToConnect(Context context, FptnServerDto fptnServerDto) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_CONNECT);
        if (fptnServerDto != null) {
            intent.putExtra(SELECTED_SERVER, fptnServerDto.id);
        }

        // If started service not become foreground - will be exception ANR - after 30 seconds approx.
        //context.startForegroundService(intent);
        context.startService(intent);
    }

    public static void startToDisconnect(Context context) {
        Intent intent = new Intent(context, CustomVpnService.class);
        intent.setAction(ACTION_DISCONNECT);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "CustomVpnService.onCreate() Thread.Id: " + Thread.currentThread().getId());
        // The handler is only used to show messages.
        if (handler == null) {
            handler = new Handler(this);
        }
        // pending intent for open MainActivity on tap
        launchMainActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        // pending intent for disconnect button in notification
        disconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, CustomVpnService.class)
                        .setAction(CustomVpnService.ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE);
        fptnServerRepository = new FptnServerRepository(getApplicationContext());

        /* check if notification allowed */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        isNotificationAllowed = notificationManager.areNotificationsEnabled();

        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand() Intent: " + intent + ", Thread.Id: " + Thread.currentThread().getId());

        if (intent != null && ACTION_RESET_ALARM.equals(intent.getAction())) {
            resetServiceKeepAliveAlarm();
            return START_NOT_STICKY;
        }

        executorService.submit(() -> {
            /* check is internet connection available */
            if (!NetworkUtils.isOnline(connectivityManager)) {
                Log.e(TAG, "onStartCommand: no active internet connections!");
                disconnect(new PVNClientException(ErrorCode.NO_ACTIVE_INTERNET_CONNECTIONS));
                return;
            }

            try {
                String sniHostname = SharedPrefUtils.getSniHostname(getApplicationContext());
                if (intent == null) {
                    /* restart after service destruction - all fields of intent is null */
                    Log.w(TAG, "onStartCommand: restart after service was killed");
                    FptnServerDto server = fptnServerRepository.getSelected().get();
                    if (server != null) {
                        connect(server, sniHostname);
                    } else {
                        /* selected server not selected - that should means that we were disconnected correct previously */
                        Log.i(TAG, "connectToPreviouslySelectedServer: previously selected server is null. No need to reconnect");
                        disconnect();
                    }
                } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
                    Log.i(TAG, "onStartCommand: disconnect!");
                    /* if we need disconnect */
                    // todo: move to settings option
                    fptnServerRepository.resetSelected();
                    // stop running threads
                    disconnect();
                } else if (ACTION_CONNECT.equals(intent.getAction())) {
                    setConnectionState(ConnectionState.CONNECTING, null);

                    int serverId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);
                    if (serverId == SELECTED_SERVER_ID_AUTO) {
                        try {
                            List<FptnServerDto> fptnServerDtos = fptnServerRepository.getServersListFuture(false).get();
                            FptnServerDto server = SpeedTestUtils.findFastestServer(fptnServerDtos, sniHostname);
                            fptnServerRepository.setIsSelected(server.id);
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

                    // for infinite run
                    resetServiceKeepAliveAlarm();
                }
            } catch (ExecutionException | InterruptedException e) {
                disconnect(new PVNClientException(e.getMessage()));
            }
        });
        // START_STICKY works not great, OS can restart service after 3 seconds or 3 minutes
        // START_NOT_STICKY - no need to restart
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");

        disconnect();
    }

    // this methods call when user remove activity from stack
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved()");
    }

    // alarming must keep service alive (maybe not)
    private void resetServiceKeepAliveAlarm() {
        Log.i(TAG, "resetAlarm()");

        ConnectionState connectionState = Optional.ofNullable(serviceStateMutableLiveData.getValue()).map(CustomVpnServiceState::getConnectionState).orElse(null);
        if (connectionState == ConnectionState.CONNECTED ||
                connectionState == ConnectionState.CONNECTING ||
                connectionState == ConnectionState.RECONNECTING) {
            Log.i(TAG, "Add restart event with one minute delay");
            AlarmManager systemService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (systemService != null) {
                Intent restartIntent = new Intent(getApplicationContext(), CustomVpnService.class);
                restartIntent.setPackage(getPackageName());
                restartIntent.setAction(CustomVpnService.ACTION_RESET_ALARM);

                PendingIntent pendingRestartIntent = PendingIntent.getService(getApplicationContext(), 1, restartIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                systemService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + ONE_MINUTE, pendingRestartIntent);
            }
        } else {
            Log.i(TAG, "No need keep alive");
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

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "handleMessage: " + message);

        /* Check connectionId in message to exclude false positive change state UI on dead connection */
        int connectionId = message.arg1;
        if (Optional.ofNullable(activeConnection.get()).map(CustomVpnConnection::getConnectionId).orElse(-1) == connectionId) {
            HandlerMessageTypes type = Arrays.stream(HandlerMessageTypes.values()).filter(t -> t.getValue() == message.what).findFirst().orElse(HandlerMessageTypes.UNKNOWN);
            switch (type) {
                case SPEED_INFO:
                    if (serviceStateMutableLiveData.getValue().getConnectionState() == ConnectionState.CONNECTED) {
                        Triple<String, String, Long> speedAndDuration = (Triple<String, String, Long>) message.obj;
                        String downloadSpeed = speedAndDuration.getFirst();
                        String uploadSpeed = speedAndDuration.getSecond();

                        // add sign to title if service in foreground
                        boolean isServiceForeground = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            isServiceForeground = ServiceUtils.isServiceForeground(getForegroundServiceType());
                        }

                        updateNotificationWithMessage(
                                String.format("%s %s (%s)", getString(R.string.connected_to), getActionConnectServerInfo(), isServiceForeground ? "Y" : "N"),
                                String.format(getString(R.string.download_upload_speed_pattern), downloadSpeed, uploadSpeed)
                        );

                        speedAndDurationMutableLiveData.postValue(speedAndDuration);
                    }
                    break;
                case CONNECTION_STATE:
                    ConnectionState connectionState = (ConnectionState) message.obj;
                    int reconnectionCount = message.arg2;

                    switchState(connectionState, reconnectionCount);
                    break;
                case ERROR:
                    PVNClientException exception = (PVNClientException) message.obj;
                    disconnect(exception);
                    if (Objects.equals(exception.errorCode, ErrorCode.RECONNECTING_FAILED)) {
                        showReconnectionFailedNotification();
                    }
                    break;
                default:
                    Log.e(TAG, "unexpected message: " + message);
            }
        }
        return true;
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

        try {
            CustomVpnConnection connection = new CustomVpnConnection(
                    this,
                    nextConnectionId.getAndIncrement(),
                    fptnServerDto,
                    sniHostname,
                    currentIPAddress,
                    networkType);
            connection.setConfigureVpnIntent(launchMainActivityPendingIntent);
            connection.start();

            setActiveConnection(connection);
        } catch (PVNClientException ex) {
            disconnect(ex);
        }
    }

    @SuppressLint("WakelockTimeout")
    private void acquirePowerLock() {
        // release previous power lock
        releasePowerLock();
        // we need this lock so our service gets not affected by Doze Mode
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FPTN_SERVICE_POWER_LOCK);
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
        if (!isNotificationAllowed) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.MAIN_CONNECTED_NOTIFICATION_ID);
    }

    private void startForegroundWithNotification(String title) {
        if (!isNotificationAllowed) {
            return;
        }

        NotificationUtils.configureNotificationChannel(this);
        Notification notification = createNotification(title, "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
        }
    }

    private void updateNotificationWithMessage(String title, String message) {
        if (!isNotificationAllowed) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, message);
        notificationManager.notify(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.disconnect_action), disconnectPendingIntent)
                .build();
        // todo: add log if service in foreground?
        Notification.Builder builder = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
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
        if (!isNotificationAllowed) {
            return;
        }

        Notification notification = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(getApplication().getString(R.string.reconnecting_failed))
                .setContentIntent(launchMainActivityPendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.INFO_NOTIFICATION_NOTIFICATION_ID, notification);
    }
}
