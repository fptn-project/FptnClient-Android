package org.fptn.vpn.views.home;

import static org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.fptn.vpn.R;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.services.vpn.FptnServiceState;

import org.fptn.vpn.utils.NetworkUtils;
import org.fptn.vpn.utils.TimeUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class HomeActivityViewModel extends AndroidViewModel {
    private static final String TAG = HomeActivityViewModel.class.getSimpleName();
    public static final int PING_DELAY_MILLIS = 2000;

    @Getter
    private final MutableLiveData<FptnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(FptnServiceState.INITIAL);

    /* Only for show on views */
    @Getter
    private final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_time));
    @Getter
    private final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");
    @Getter
    private final MutableLiveData<String> statusTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.disconnected));
    @Getter
    private final MutableLiveData<List<ServerEntity>> serverDtoListLiveData = new MutableLiveData<>(List.of());
    @Getter
    private final MutableLiveData<String> connectedServerInfoLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ExecutorService pingExecutorService = Executors.newSingleThreadExecutor();
    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    // observers
    private final Observer<FptnServiceState> serviceStateObserver;

    private final ConnectivityManager connectivityManager;
    private volatile boolean isPingCheckingActive = false;

    public HomeActivityViewModel(@NonNull Application application) {
        super(application);

        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);

        serviceStateObserver = fptnServiceState -> {
            if (fptnServiceState != null) {
                ConnectionState connectionState = fptnServiceState.getConnectionState();
                switch (connectionState) {
                    case DISCONNECTED ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.disconnected));
                    case CONNECTING -> {
                        statusTextLiveData.postValue(getApplication().getString(R.string.connecting));
                        resetErrorMessage();
                    }
                    case CONNECTED -> {
                        statusTextLiveData.postValue(getApplication().getString(R.string.connected));
                        updateServers();
                        resetErrorMessage();
                    }
                    case RECONNECTING ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.connected));
                }

                if (!connectionState.isActiveState()) {
                    startCheckingPing();
                } else {
                    stopCheckingPing();
                }

                PVNClientException exception = fptnServiceState.getException();
                if (exception != null) {
                    handlePVNClientException(exception);
                }
            }
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);

        updateServers();
    }

    private void startCheckingPing() {
        // Prevent multiple loops from starting
        if (isPingCheckingActive) return;
        isPingCheckingActive = true;

        Log.d(TAG, "startCheckingPing");

        pingExecutorService.submit(() -> {
            while (isPingCheckingActive) {
                Log.d(TAG, "Checking ping...");
                List<ServerEntity> servers = serverDtoListLiveData.getValue();
                if (servers == null || servers.isEmpty()) {
                    isPingCheckingActive = false;
                    break;
                }

                // Check is internet connection available
                if (NetworkUtils.isOnline(connectivityManager)) {
                    // Create a temporary pool for this batch of pings
                    ExecutorService batchPingExecutor = Executors.newFixedThreadPool(servers.size());
                    try {
                        List<CompletableFuture<Void>> futures = new ArrayList<>();

                        for (ServerEntity server : servers) {
                            if (server == ServerEntity.AUTO) continue;

                            futures.add(CompletableFuture.runAsync(() -> {

                                long startTime = System.currentTimeMillis();
                                try (Socket socket = new Socket()) {
                                    // Use port 443 or your specific VPN port
                                    socket.connect(new InetSocketAddress(server.getHost(), 443), 2000);
                                    server.setPingMs(System.currentTimeMillis() - startTime);

                                    Log.d(TAG, "Ping for host: " + server.getServerInfo() + " ping: " + server.getPingMs() + "ms");
                                } catch (IOException e) {
                                    server.setPingMs(-1);
                                }
                            }, batchPingExecutor));
                        }

                        // Wait for the whole batch to finish
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


                    } finally {
                        batchPingExecutor.shutdown();
                    }
                } else {
                    Log.d(TAG, "startCheckingPing: no active internet connections!");
                    for (ServerEntity server : servers) {
                        server.setPingMs(-1);
                    }
                }

                // Update the UI with new ping values
                serverDtoListLiveData.postValue(servers);

                // Wait before the next check
                try {
                    Thread.sleep(PING_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isPingCheckingActive = false;
                }
            }
        });
    }

    private void stopCheckingPing() {
        isPingCheckingActive = false;
    }

    public void updateServers() {
        executorService.submit(() -> {
            List<ServerEntity> serverList = new ArrayList<>();
            serverList.add(ServerEntity.AUTO);
            serverList.addAll(appDatabase.serverDAO().getServerList());

            serverList.stream().filter(ServerEntity::isSelected).findFirst()
                    .ifPresent(serverEntity ->
                            connectedServerInfoLiveData.postValue(serverEntity.getServerInfo()));

            serverDtoListLiveData.postValue(serverList);
        });
    }

    private void handlePVNClientException(PVNClientException exception) {
        ErrorCode errorCode = exception.errorCode;
        if (errorCode != ErrorCode.UNKNOWN_ERROR) {
            String stringResourceByName = getStringResourceByName(getApplication(), errorCode.getValue());
            Log.e(TAG, "Error as text: " + stringResourceByName);

            errorTextLiveData.postValue(stringResourceByName);
        } else {
            errorTextLiveData.postValue(exception.errorMessage);
        }
    }

    public void resetErrorMessage() {
        errorTextLiveData.postValue("");
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        serviceStateMutableLiveData.removeObserver(serviceStateObserver);
    }

    public void subscribeService(FptnService service) {
        service.getSpeedAndDurationMutableLiveData().observeForever(speedAndDuration -> {
            if (speedAndDuration != null) {
                downloadSpeedAsStringLiveData.postValue(speedAndDuration.getFirst());
                uploadSpeedAsStringLiveData.postValue(speedAndDuration.getSecond());
                timerTextLiveData.postValue(TimeUtils.getTime(speedAndDuration.getThird()));
            }
        });

        service.getServiceStateMutableLiveData().observeForever(serviceStateMutableLiveData::postValue);
    }

    public void unsubscribe() {
        // todo: check memory leaks and maybe remove observers
    }
}
