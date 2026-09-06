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

package org.fptn.vpn.ui.home;

import static org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.elvishew.xlog.XLog;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class HomeActivityViewModel extends AndroidViewModel {
    private static final String TAG = HomeActivityViewModel.class.getSimpleName();

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
    @Getter
    private final MutableLiveData<String> downloadTrafficLiveData = new MutableLiveData<>("0 B");
    @Getter
    private final MutableLiveData<String> uploadTrafficLiveData = new MutableLiveData<>("0 B");
    @Getter
    private final MutableLiveData<long[]> speedSampleLiveData = new MutableLiveData<>();

    // Written out explicitly (instead of relying on Lombok's @Getter) for the LiveData fields
    // the Compose HomeScreen needs: Kotlin's Java-interop stub generation runs before the
    // Lombok annotation processor, so Kotlin call sites can't see Lombok-generated accessors.
    public MutableLiveData<FptnServiceState> getServiceStateMutableLiveData() {
        return serviceStateMutableLiveData;
    }

    public MutableLiveData<String> getTimerTextLiveData() {
        return timerTextLiveData;
    }

    public MutableLiveData<String> getDownloadSpeedAsStringLiveData() {
        return downloadSpeedAsStringLiveData;
    }

    public MutableLiveData<String> getUploadSpeedAsStringLiveData() {
        return uploadSpeedAsStringLiveData;
    }

    public MutableLiveData<String> getStatusTextLiveData() {
        return statusTextLiveData;
    }

    public MutableLiveData<List<ServerEntity>> getServerDtoListLiveData() {
        return serverDtoListLiveData;
    }

    public MutableLiveData<String> getConnectedServerInfoLiveData() {
        return connectedServerInfoLiveData;
    }

    public MutableLiveData<String> getDownloadTrafficLiveData() {
        return downloadTrafficLiveData;
    }

    public MutableLiveData<String> getUploadTrafficLiveData() {
        return uploadTrafficLiveData;
    }

    public MutableLiveData<long[]> getSpeedSampleLiveData() {
        return speedSampleLiveData;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    // observers
    private final Observer<FptnServiceState> serviceStateObserver;

    // for pingers
    public static volatile List<ServerEntity> lastPingedServers = null;
    private final ConnectivityManager connectivityManager;
    private final ExecutorService pingExecutorService = Executors.newSingleThreadExecutor();
    private volatile boolean isPingCheckingActive = false;
    public static final int PING_DELAY_MILLIS = 1000;
    public static final int BATCH_SIZE = 8;
    private static final long PING_CYCLE_INTERVAL_MILLIS = 5 * 60 * 1000L;
    private volatile long lastPingCycleTime = 0L;

    public HomeActivityViewModel(@NonNull Application application) {
        super(application);

        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);

        serviceStateObserver = fptnServiceState -> {
            if (fptnServiceState != null) {
                ConnectionState connectionState = fptnServiceState.getConnectionState();
                switch (connectionState) {
                    case DISCONNECTED -> {
                        String reason = fptnServiceState.getDisconnectReason();
                        String label = (reason != null && !reason.isEmpty())
                                ? reason
                                : getApplication().getString(R.string.disconnected);
                        statusTextLiveData.postValue(label);
                        resetErrorMessage();
                        refreshServerListFromDB();
                    }
                    case WAITING_FOR_NETWORK ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.waiting_for_network));
                    case CONNECTING ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.connecting));
                    case CONNECTED ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.connected));
                    case BLOCKED ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.kill_switch_blocked));
                    case RECONNECTING -> {} // text comes from exception below
                }

                PVNClientException exception = fptnServiceState.getException();
                if (exception != null && connectionState != ConnectionState.BLOCKED) {
                    handlePVNClientException(exception);
                }
            }
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
    }

    public void startCheckingPing() {
        // Prevent multiple loops from starting
        if (isPingCheckingActive) return;
        isPingCheckingActive = true;

        XLog.tag(TAG).i("Ping check loop started");

        pingExecutorService.submit(() -> {
            while (isPingCheckingActive) {
                List<ServerEntity> servers = serverDtoListLiveData.getValue();
                if (servers == null || servers.isEmpty()) {
                    try {
                        Thread.sleep(PING_DELAY_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        isPingCheckingActive = false;
                    }
                    continue;
                }

                long now = System.currentTimeMillis();
                if (lastPingCycleTime != 0 && now - lastPingCycleTime < PING_CYCLE_INTERVAL_MILLIS) {
                    try {
                        Thread.sleep(PING_DELAY_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        isPingCheckingActive = false;
                    }
                    continue;
                }
                lastPingCycleTime = now;

                XLog.tag(TAG).d("Ping cycle triggered [servers=%d]", servers.size());

                // Check is internet connection available
                if (NetworkUtils.isOnline(connectivityManager)) {
                    // Filter out AUTO and create a working list to avoid concurrent modification issues
                    List<ServerEntity> targets = new ArrayList<>();
                    for (ServerEntity s : servers) {
                        if (s != ServerEntity.AUTO) {
                            targets.add(s);
                        }
                    }

                    // Process in batches
                    for (int i = 0; i < targets.size(); i += BATCH_SIZE) {
                        // Calculate the end of the current batch
                        int end = Math.min(i + BATCH_SIZE, targets.size());
                        List<ServerEntity> batch = targets.subList(i, end);

                        // Use a temporary executor for the current batch
                        ExecutorService batchPingExecutor = Executors.newFixedThreadPool(batch.size());
                        try {
                            List<CompletableFuture<Void>> futures = new ArrayList<>();

                            for (ServerEntity server : batch) {
                                futures.add(CompletableFuture.runAsync(() -> {
                                    long startTime = System.currentTimeMillis();
                                    try (Socket socket = new Socket()) {
                                        // Connect with a timeout
                                        socket.connect(new InetSocketAddress(server.getHost(), server.getPort()), 5000);
                                        server.setPingMs(System.currentTimeMillis() - startTime);

                                        XLog.tag(TAG).d("Ping result [server=%s, ms=%d]", server.getServerInfo(), server.getPingMs());
                                    } catch (IOException e) {
                                        server.setPingMs(-1);
                                    }
                                }, batchPingExecutor));
                            }

                            // Wait for the current batch to finish before starting the next
                            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                        } finally {
                            batchPingExecutor.shutdown();
                        }

                        // Update the UI with new ping values
                        lastPingedServers = servers;
                        serverDtoListLiveData.postValue(servers);

                        // Wait before take nest batch
                        try {
                            Thread.sleep(PING_DELAY_MILLIS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            isPingCheckingActive = false;
                        }
                    }
                } else {
                    XLog.tag(TAG).w("No active internet connection — skipping ping cycle");
                    for (ServerEntity server : servers) {
                        server.setPingMs(-1);
                    }
                }
            }
        });
    }

    public void stopCheckingPing() {
        isPingCheckingActive = false;
    }

    public void refreshServerListFromDB() {
        executorService.submit(() -> {
            List<ServerEntity> serverList = new ArrayList<>();
            serverList.add(ServerEntity.AUTO);
            serverList.addAll(appDatabase.serverDAO().getServerList());

            // copy previous pings
            if (lastPingedServers != null) {
                for (ServerEntity loaded : serverList) {
                    for (ServerEntity pinged : lastPingedServers) {
                        if (loaded.getHost().equals(pinged.getHost()) && loaded.getPort() == pinged.getPort()) {
                            loaded.setPingMs(pinged.getPingMs());
                            break;
                        }
                    }
                }
            }
            serverDtoListLiveData.postValue(serverList);
        });
    }

    private void handlePVNClientException(PVNClientException exception) {
        ErrorCode errorCode = exception.errorCode;
        if (errorCode != ErrorCode.UNKNOWN_ERROR) {
            String msg = getStringResourceByName(getApplication(), errorCode.getValue());
            XLog.tag(TAG).e("VPN error [code=%s]: %s", errorCode, msg);
            statusTextLiveData.postValue(msg);
        } else {
            statusTextLiveData.postValue(exception.errorMessage);
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

        service.getTrafficBytesLiveData().observeForever(traffic -> {
            if (traffic != null) {
                downloadTrafficLiveData.postValue(formatBytes(traffic.getFirst()));
                uploadTrafficLiveData.postValue(formatBytes(traffic.getSecond()));
            }
        });

        service.getRawSpeedBpsLiveData().observeForever(bps -> {
            if (bps != null) {
                speedSampleLiveData.postValue(bps);
            }
        });

        service.getServiceStateMutableLiveData().observeForever(serverState -> {
            serviceStateMutableLiveData.postValue(serverState);

            Optional.ofNullable(serverState).map(FptnServiceState::getConnectionState)
                    .filter(ConnectionState::isActiveState)
                    .ifPresent(state -> {
                        String info = serverState.getServerInfo() != null
                                ? serverState.getServerInfo()
                                : service.getActionConnectServerInfo();
                        connectedServerInfoLiveData.postValue(info);
                    });
        });
    }

    public void unsubscribe() {
        // todo: check memory leaks and maybe remove observers
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000L) {
            return String.format("%.2f GB", bytes / 1_000_000_000.0);
        } else if (bytes >= 1_000_000L) {
            return String.format("%.2f MB", bytes / 1_000_000.0);
        } else if (bytes >= 1_000L) {
            return String.format("%.2f KB", bytes / 1_000.0);
        } else {
            return bytes + " B";
        }
    }
}
