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

import static org.fptn.vpn.enums.ConnectionSubnets.ALL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.FPTN_SERVER_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.IP_V4_PREFIX_LENGTH;
import static org.fptn.vpn.enums.ConnectionSubnets.IP_V6_PREFIX_LENGTH;
import static org.fptn.vpn.enums.ConnectionSubnets.LOCAL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.LOCAL_TUN_INTERFACE_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.TUN_ADDRESS;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.NetworkType;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.websocket.DnsServers;
import org.fptn.vpn.services.websocket.WebSocketAlreadyShutdownException;
import org.fptn.vpn.services.websocket.WebSocketClientWrapper;
import org.fptn.vpn.utils.DataRateCalculator;
import org.fptn.vpn.utils.NetworkUtils;
import org.fptn.vpn.utils.IPUtils;
import org.fptn.vpn.views.perappvpn.AppInfo;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Getter;
import lombok.Setter;

public class FptnConnection extends Thread {
    private static final String TAG = FptnConnection.class.getSimpleName();

    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 1500;

    @Getter
    private final int connectionId;
    private final FptnService service;

    @Getter
    private final ServerEntity serverEntity;
    private final WebSocketClientWrapper webSocketClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);
    private final Thread currentThread = this;

    @Getter
    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    @Setter
    private PendingIntent configureVpnIntent;

    private ParcelFileDescriptor vpnInterface;
    private FileOutputStream outputStream;

    private volatile boolean tunNeedsRecreate = false;

    @Getter
    private Instant connectionTime;
    private ScheduledFuture<?> onFailureScheduledTask;
    @Getter
    @Setter
    private String currentIPAddress;
    @Getter
    @Setter
    private NetworkType currentNetworkType;

    private final List<String> allServerHosts;
    private final int maxReconnectCount;
    private final int fallbackThreshold; // 0 = disabled
    private final int delayBetweenAttempts;
    private final PerAppVpnMode perAppVpnMode;
    private final List<AppInfo> appInfos;
    private final ConnectivityManager connectivityManager;

    private final Object webSocketLock = new Object();

    public FptnConnection(final FptnService service,
                          final int connectionId,
                          final ServerEntity serverEntity,
                          final List<String> allServerHosts,
                          final String currentIPAddress,
                          final NetworkType currentNetworkType,
                          final int maxReconnectCount,
                          final int fallbackThreshold,
                          final int delayBetweenAttempts,
                          final String sniHostName,
                          final BypassCensorshipMethod censorshipStrategy,
                          final SniSpoofingMode sniSpoofingMode,
                          final PerAppVpnMode perAppVpnMode,
                          final List<AppInfo> appInfos,
                          final String preFetchedToken,
                          final ConnectivityManager connectivityManager) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverEntity = serverEntity;
        this.allServerHosts = allServerHosts;
        this.currentIPAddress = currentIPAddress;
        this.currentNetworkType = currentNetworkType;
        this.perAppVpnMode = perAppVpnMode;
        this.appInfos = appInfos;
        this.connectivityManager = connectivityManager;
        this.maxReconnectCount = maxReconnectCount;
        this.fallbackThreshold = fallbackThreshold;
        this.delayBetweenAttempts = delayBetweenAttempts;
        this.webSocketClient = new WebSocketClientWrapper(
                this.serverEntity,
                TUN_ADDRESS.getIpV4Address(),
                TUN_ADDRESS.getIpV6Address(),
                this::onConnectionOpen,
                this::onMessageReceived,
                this::onConnectionFailure,
                sniHostName,
                censorshipStrategy,
                sniSpoofingMode,
                preFetchedToken
        );
    }

    @Override
    public void run() {
        XLog.tag(TAG).i("Connection started [id=%d, server=%s, thread=%d]",
                connectionId, serverEntity.getServerInfo(), Thread.currentThread().getId());
        try {
            sendConnectionStateToService(ConnectionState.CONNECTING);

            setupTun();
            webSocketClient.startWebSocket();
            configureConnectionTimeSpeedScheduler();

            byte[] byteBuffer = new byte[MAX_PACKET_SIZE];
            while (!currentThread.isInterrupted() && runTunReadLoop(byteBuffer)) {}
        } catch (PVNClientException e) {
            sendExceptionToService(e);
        } catch (IOException ex) {
            XLog.tag(TAG).w("[id=" + connectionId + "] TUN interface closed: " + ex.getMessage());
        } catch (WebSocketAlreadyShutdownException e) {
            XLog.tag(TAG).w("The websocket already shutdown", e);
        } catch (InterruptedException e) {
            XLog.tag(TAG).d("InterruptedException catch!", e);
        } catch (Exception e) {
            XLog.tag(TAG).e("[id=" + connectionId + "] ERRORRR [wsStarted=" + webSocketClient.isStarted() + "]: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void setupTun() throws UnknownHostException, PVNClientException {
        if (outputStream != null) {
            try { outputStream.close(); } catch (IOException ignored) {}
            outputStream = null;
        }
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }

        VpnService.Builder builder = service.new Builder();
        builder.setMtu(MAX_PACKET_SIZE);
        builder.setBlocking(true);
        builder.setConfigureIntent(configureVpnIntent);
        configurePerAppMode(builder);
        configureAddressesAndRoutes(builder);

        synchronized (service) {
            vpnInterface = builder.establish();
        }

        XLog.tag(TAG).i("[id=%d] TUN interface established [fd=%s]", connectionId, vpnInterface);
        if (isTunInterfaceValid(vpnInterface)) {
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
        } else {
            throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR);
        }
    }

    private boolean runTunReadLoop(byte[] byteBuffer) throws InterruptedException, WebSocketAlreadyShutdownException {
        try (FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor())) {
            while (!currentThread.isInterrupted()) {
                int length = inputStream.read(byteBuffer);
                uploadRate.update(length);
                if (!webSocketClient.isStarted()) {
                    synchronized (webSocketLock) {
                        webSocketLock.wait();
                    }
                }
                webSocketClient.send(byteBuffer, length);
            }
        } catch (IOException ex) {
            if (tunNeedsRecreate && !currentThread.isInterrupted()) {
                XLog.tag(TAG).i("[id=%d] TUN closed — recreating interface", connectionId);
                try {
                    setupTun();
                    tunNeedsRecreate = false;
                    XLog.tag(TAG).i("[id=%d] TUN interface recreated successfully", connectionId);
                    return true;
                } catch (Exception e) {
                    XLog.tag(TAG).e("[id=%d] Failed to recreate TUN: %s", connectionId, e.getMessage());
                }
            } else {
                XLog.tag(TAG).w("[id=%d] TUN interface closed: %s", connectionId, ex.getMessage());
            }
        }
        return false;
    }

    private void configurePerAppMode(VpnService.Builder builder) {
        // Per-app routing
        if (perAppVpnMode == PerAppVpnMode.OFF) {
            String thisAppPackageName = service.getPackageName();
            try {
                builder.addDisallowedApplication(thisAppPackageName); // todo: MAYBE something wrong with routes
            } catch (PackageManager.NameNotFoundException e) {
                XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, thisAppPackageName);
            }
        } else if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
            if (appInfos.isEmpty()) {
                // No apps selected: add self to allowed list so no user traffic is tunneled
                String thisAppPackageName = service.getPackageName();
                try {
                    builder.addAllowedApplication(thisAppPackageName);
                } catch (PackageManager.NameNotFoundException e) {
                    XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, thisAppPackageName);
                }
            } else {
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    try {
                        builder.addAllowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, packageName);
                    }
                }
            }
        } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED) {
            for (AppInfo appInfo : appInfos) {
                String packageName = appInfo.getPackageName();
                try {
                    builder.addDisallowedApplication(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, packageName);
                }
            }
        }
    }

    public void shutdown() {
        XLog.tag(TAG).i("[id=%d] Shutting down [alreadyInterrupted=%b]",
                connectionId, currentThread.isInterrupted());
        if (!currentThread.isInterrupted()) {
            currentThread.interrupt();
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                XLog.tag(TAG).d("Unable to close output stream", e);
            }
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                XLog.tag(TAG).d("Unable to close interface", e);
            }
        }
        webSocketClient.shutdown();
        scheduler.shutdown();

        sendConnectionStateToService(ConnectionState.DISCONNECTED);
    }

    private void configureConnectionTimeSpeedScheduler() {
        try {
            connectionTime = Instant.now();
            scheduler.scheduleWithFixedDelay(() -> {
                String downloadSpeed = downloadRate.getFormatString();
                String uploadSpeed = uploadRate.getFormatString();
                long durationInSeconds = (int) Duration.between(connectionTime, Instant.now()).getSeconds();
                sendSpeedInfoAndDurationToService(downloadSpeed, uploadSpeed, durationInSeconds);
            }, 1, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            XLog.tag(TAG).i("update speed task rejected by scheduler", e);
        }
    }

    private void configureAddressesAndRoutes(VpnService.Builder builder) throws UnknownHostException, PVNClientException {
        final DnsServers dnsServers = webSocketClient.getDnsServers();

        // IPv4
        builder.addDnsServer(dnsServers.getIpv4());
        builder.addAddress(TUN_ADDRESS.getIpV4Address(), IP_V4_PREFIX_LENGTH);
        builder.addRoute(dnsServers.getIpv4(), IP_V4_PREFIX_LENGTH);

        // IPv6
        builder.addDnsServer(dnsServers.getIpv6());
        builder.addAddress(TUN_ADDRESS.getIpV6Address(), IP_V6_PREFIX_LENGTH);
        builder.addRoute(dnsServers.getIpv6(), IP_V6_PREFIX_LENGTH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String host : allServerHosts) {
                try {
                    builder.excludeRoute(new IpPrefix(InetAddress.getByName(host), IP_V4_PREFIX_LENGTH));
                } catch (UnknownHostException e) {
                    XLog.tag(TAG).w("[id=%d] Cannot exclude server from TUN routing: %s", connectionId, host);
                }
            }
            builder.excludeRoute(LOCAL_TUN_INTERFACE_SUBNET.getAsIpV4Prefix());
            builder.excludeRoute(LOCAL_TUN_INTERFACE_SUBNET.getAsIpV6Prefix());
            builder.excludeRoute(FPTN_SERVER_SUBNET.getAsIpV4Prefix());
            builder.excludeRoute(FPTN_SERVER_SUBNET.getAsIpV6Prefix());
            builder.excludeRoute(LOCAL_SUBNET.getAsIpV4Prefix());
            builder.excludeRoute(LOCAL_SUBNET.getAsIpV6Prefix());
            builder.addRoute(ALL_SUBNET.getIpV4Address(), ALL_SUBNET.getV4prefix());
            builder.addRoute(ALL_SUBNET.getIpV6Address(), ALL_SUBNET.getV6prefix());
        } else {
            // IPv4
            IPAddress rootSubnetV4 = new IPAddressString(ALL_SUBNET.getAsIpV4PrefixAsString()).getAddress();
            List<IPAddress> subnetsToExcludeV4 = Stream.concat(
                            allServerHosts.stream().map(host -> String.format("%s/%s", host, IP_V4_PREFIX_LENGTH)),
                            Stream.of(
                                    LOCAL_TUN_INTERFACE_SUBNET.getAsIpV4PrefixAsString(),
                                    FPTN_SERVER_SUBNET.getAsIpV4PrefixAsString(),
                                    LOCAL_SUBNET.getAsIpV4PrefixAsString()
                            )
                    )
                    .map(sub -> new IPAddressString(sub).getAddress())
                    .collect(Collectors.toList());

            List<IPAddress> subnetsToIncludeV4 = new ArrayList<>();
            IPUtils.exclude(rootSubnetV4, subnetsToExcludeV4, subnetsToIncludeV4, IP_V4_PREFIX_LENGTH);
            for (IPAddress ipAddress : subnetsToIncludeV4) {
                String hostIp = ipAddress.getLower().toAddressString().getHostAddress().toString();
                Integer networkPrefixLength = ipAddress.getLower().toAddressString().getNetworkPrefixLength();
                XLog.tag(TAG).d("[id=%d] IPv4 route added [subnet=%s/%s]", connectionId, hostIp, networkPrefixLength);
                builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : IP_V4_PREFIX_LENGTH);
            }

            // IPv6
            IPAddress rootSubnetV6 = new IPAddressString(ALL_SUBNET.getAsIpV6PrefixAsString()).getAddress();
            XLog.tag(TAG).d("[id=%d] IPv6 root subnet [subnet=%s]", connectionId, rootSubnetV6);
            List<IPAddress> subnetsToExcludeV6 = Stream.of(
                            LOCAL_TUN_INTERFACE_SUBNET.getAsIpV6PrefixAsString(),
                            FPTN_SERVER_SUBNET.getAsIpV6PrefixAsString(),
                            LOCAL_SUBNET.getAsIpV6PrefixAsString()
                    )
                    .map(sub -> new IPAddressString(sub).getAddress())
                    .collect(Collectors.toList());

            List<IPAddress> subnetsToIncludeV6 = new ArrayList<>();
            IPUtils.exclude(rootSubnetV6, subnetsToExcludeV6, subnetsToIncludeV6, IP_V6_PREFIX_LENGTH);
            for (IPAddress ipAddress : subnetsToIncludeV6) {
                String hostIp = ipAddress.getLower().toAddressString().getHostAddress().toString();
                Integer networkPrefixLength = ipAddress.getLower().toAddressString().getNetworkPrefixLength();
                XLog.tag(TAG).d("[id=%d] IPv6 route added [subnet=%s/%s]", connectionId, hostIp, networkPrefixLength);
                builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : IP_V6_PREFIX_LENGTH);
            }
        }
    }

    private void onConnectionOpen() {
        XLog.tag(TAG).i("[id=%d] WebSocket connected [reconnectCount=%d]",
                connectionId, reconnectCount.get());
        reconnectCount.set(0);
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            cancelReconnectTask();

            // resume thread
            synchronized (webSocketLock) {
                webSocketLock.notify();
            }
        }
    }

    private void onMessageReceived(byte[] data) {
        try {
            if (outputStream != null) {
                downloadRate.update(data.length);
                outputStream.write(data);
            }
        } catch (Exception e) {
            XLog.tag(TAG).w("[id=%d] Failed to write %d-byte packet to TUN: %s",
                    connectionId, data.length, e.getMessage());
            if (!tunNeedsRecreate && !currentThread.isInterrupted()) {
                tunNeedsRecreate = true;
                if (vpnInterface != null) {
                    try { vpnInterface.close(); } catch (IOException ignored) {}
                }
            }
        }
    }

    public void onConnectionFailure() {
        XLog.tag(TAG).w("[id=%d] Connection failure detected [wsStarted=%b, tunValid=%b, reconnectActive=%b]",
                connectionId,
                webSocketClient.isStarted(),
                isTunInterfaceValid(vpnInterface),
                onFailureScheduledTask != null && !onFailureScheduledTask.isCancelled());
        cancelReconnectTask();
        webSocketClient.stopWebSocket();
        try {
            onFailureScheduledTask = scheduler.scheduleWithFixedDelay(() -> {
                if (webSocketClient.isStarted()) {
                    XLog.tag(TAG).i("[id=%d] WebSocket already reconnected by native layer — cancelling Java retry", connectionId);
                    cancelReconnectTask();
                    return;
                }
                if (!NetworkUtils.isOnline(connectivityManager)) {
                    XLog.tag(TAG).i("[id=%d] No internet — suspending reconnect, entering WAITING_FOR_NETWORK", connectionId);
                    service.enterWaitingForNetwork(serverEntity.getId());
                    return;
                }
                int currentCount = reconnectCount.incrementAndGet();
                XLog.tag(TAG).i("[id=%d] Reconnecting [attempt %d/%d]", connectionId, currentCount, maxReconnectCount);
                if (!currentThread.isInterrupted() && isTunInterfaceValid(vpnInterface) && currentCount <= maxReconnectCount) {
                    if (fallbackThreshold > 0 && currentCount >= fallbackThreshold) {
                        XLog.tag(TAG).i("[id=%d] Fallback triggered [attempt %d/%d] — requesting all-server scan",
                                connectionId, currentCount, fallbackThreshold);
                        sendExceptionToService(new PVNClientException(ErrorCode.FALLBACK_TO_ALL_SERVERS));
                        onFailureInterrupt();
                        return;
                    }
                    try {
                        sendConnectionStateToService(ConnectionState.RECONNECTING, currentCount);
                        webSocketClient.startWebSocket();
                        cancelReconnectTask();
                    } catch (PVNClientException e) {
                        if (e.errorCode == ErrorCode.ACCESS_TOKEN_ERROR) {
                            webSocketClient.invalidateAccessToken();
                        }
                        if (e.errorCode == ErrorCode.ACCESS_TOKEN_ERROR || currentCount == maxReconnectCount) {
                            sendExceptionToService(e);
                            onFailureInterrupt();
                        }
                    } catch (WebSocketAlreadyShutdownException e) {
                        XLog.tag(TAG).w("[id=%d] WebSocket already shut down — aborting reconnect", connectionId);
                        onFailureInterrupt();
                    }
                } else {
                    XLog.tag(TAG).e("[id=%d] Reconnect limit reached [%d/%d] — giving up",
                            connectionId, currentCount, maxReconnectCount);
                    sendExceptionToService(new PVNClientException(ErrorCode.RECONNECTING_FAILED));
                    onFailureInterrupt();
                }
            }, (long) delayBetweenAttempts, delayBetweenAttempts, TimeUnit.SECONDS);
        } catch (RejectedExecutionException exception) {
            XLog.tag(TAG).e("[id=%d] Reconnect task rejected by scheduler — connection lost", connectionId);
        }
    }

    private void onFailureInterrupt() {
        if (!currentThread.isInterrupted()) {
            currentThread.interrupt();
        }
        cancelReconnectTask();
    }

    private void cancelReconnectTask() {
        if (onFailureScheduledTask != null && !onFailureScheduledTask.isCancelled()) {
            onFailureScheduledTask.cancel(true);
            onFailureScheduledTask = null;
        }
    }

    private void sendExceptionToService(PVNClientException exception) {
        service.sendExceptionToService(exception);
    }

    private void sendSpeedInfoAndDurationToService(String downloadSpeed, String uploadSpeed, long duration) {
        service.updateSpeedInfo(downloadSpeed, uploadSpeed, duration);
    }

    private void sendConnectionStateToService(ConnectionState connectionState) {
        service.updateConnectionState(connectionState, reconnectCount.get(), connectionId);
    }

    private void sendConnectionStateToService(ConnectionState connectionState, int count) {
        service.updateConnectionState(connectionState, count, connectionId);
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface != null && vpnInterface.getFileDescriptor() != null && vpnInterface.getFileDescriptor().valid();
    }

    public void onNetworkChanged() {
        XLog.tag(TAG).i("[id=%d] Network changed — forcing clean reconnect [wsStarted=%b]",
                connectionId, webSocketClient.isStarted());
        reconnectCount.set(0);
        onConnectionFailure();
    }
}
