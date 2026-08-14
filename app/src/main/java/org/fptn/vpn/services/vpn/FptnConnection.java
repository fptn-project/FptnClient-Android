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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.utils.AppExclusion;
import org.fptn.vpn.domainblocker.DomainBlocker;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionStrategy;
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
import java.util.concurrent.atomic.AtomicLong;
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

    /**
     * Google apps (YouTube, Gmail, ...) rely on these companion packages; without
     * them in the tunnel they break in allowed-only mode. Tunneled implicitly.
     */
    private static final String[] GOOGLE_SERVICE_PACKAGES = {
            "com.google.android.gms",   // Play Services
            "com.google.android.gsf",   // Services Framework
            "com.android.vending"       // Play Store
    };

    @Getter
    private final int connectionId;
    private final FptnService service;

    @Getter
    private final ServerEntity serverEntity;
    private final WebSocketClientWrapper webSocketClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);
    private final AtomicLong totalDownloadBytes = new AtomicLong(0);
    private final AtomicLong totalUploadBytes = new AtomicLong(0);
    private final Thread currentThread = this;

    @Getter
    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    @Setter
    private PendingIntent configureVpnIntent;

    private volatile ParcelFileDescriptor vpnInterface;
    private FileOutputStream outputStream;

    private volatile boolean tunNeedsRecreate = false;
    private volatile String pendingShutdownReason = null;

    @Getter
    private Instant connectionTime;
    private ScheduledFuture<?> onFailureScheduledTask;
    @Getter
    @Setter
    private String currentIPAddress;
    @Getter
    @Setter
    private NetworkType currentNetworkType;

    private final int maxReconnectCount;
    private final int fallbackThreshold; // 0 = disabled
    private final int delayBetweenAttempts;
    private final PerAppVpnMode perAppVpnMode;
    private final List<AppInfo> appInfos;
    private final ConnectivityManager connectivityManager;
    private final DomainBlocker domainBlocker; // null when ad blocking and domain blacklist are disabled
    private final String customDnsIpv4; // null when custom DNS is disabled

    public FptnConnection(final FptnService service,
                          final int connectionId,
                          final ServerEntity serverEntity,
                          final String currentIPAddress,
                          final NetworkType currentNetworkType,
                          final int maxReconnectCount,
                          final int fallbackThreshold,
                          final int delayBetweenAttempts,
                          final String sniHostName,
                          final BypassCensorshipMethod censorshipStrategy,
                          final SniSpoofingMode sniSpoofingMode,
                          final ConnectionStrategy connectionStrategy,
                          final PerAppVpnMode perAppVpnMode,
                          final List<AppInfo> appInfos,
                          final String preFetchedToken,
                          final ConnectivityManager connectivityManager,
                          final DomainBlocker domainBlocker,
                          final String customDnsIpv4,
                          final DnsServers preFetchedDnsServers) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverEntity = serverEntity;
        this.currentIPAddress = currentIPAddress;
        this.currentNetworkType = currentNetworkType;
        this.perAppVpnMode = perAppVpnMode;
        this.appInfos = appInfos;
        this.connectivityManager = connectivityManager;
        this.domainBlocker = domainBlocker;
        this.customDnsIpv4 = customDnsIpv4;
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
                this::protectSocket,
                sniHostName,
                censorshipStrategy,
                sniSpoofingMode,
                connectionStrategy,
                preFetchedToken,
                preFetchedDnsServers
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
            XLog.tag(TAG).w("[id=%d] DISCONNECT REASON: VPN client error [code=%s, msg=%s]",
                    connectionId, e.errorCode, e.errorMessage);
            sendExceptionToService(e);
        } catch (IOException ex) {
            XLog.tag(TAG).w("[id=%d] DISCONNECT REASON: IO error — VPN likely revoked by system or another app [%s]",
                    connectionId, ex.getMessage());
            pendingShutdownReason = FptnService.DISCONNECT_REASON_SYSTEM_REVOKED;
        } catch (WebSocketAlreadyShutdownException e) {
            XLog.tag(TAG).w("[id=%d] DISCONNECT REASON: WebSocket already shut down", connectionId);
            pendingShutdownReason = FptnService.DISCONNECT_REASON_CLOSED_UNEXPECTEDLY;
        } catch (InterruptedException e) {
            XLog.tag(TAG).i("[id=%d] DISCONNECT REASON: thread interrupted — explicit disconnect or service stopped", connectionId);
        } catch (Exception e) {
            XLog.tag(TAG).e("[id=%d] DISCONNECT REASON: unexpected exception [type=%s, msg=%s, wsStarted=%b]",
                    connectionId, e.getClass().getSimpleName(), e.getMessage(), webSocketClient.isStarted());
            pendingShutdownReason = FptnService.DISCONNECT_REASON_UNEXPECTED_ERROR;
        } finally {
            shutdown();
        }
    }

    private void setupTun() throws UnknownHostException, PVNClientException {
        if (outputStream != null) {
            try { outputStream.close(); } catch (IOException ignored) {}
            outputStream = null;
        }

        VpnService.Builder builder = service.new Builder();
        builder.setMtu(MAX_PACKET_SIZE);
        builder.setBlocking(true);
        builder.setConfigureIntent(configureVpnIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }
        configurePerAppMode(builder);
        configureAddressesAndRoutes(builder);

        ParcelFileDescriptor previous;
        synchronized (service) {
            previous = vpnInterface;
            vpnInterface = builder.establish();
        }
        if (previous != null) {
            try { previous.close(); } catch (IOException ignored) {}
        }

        XLog.tag(TAG).i("[id=%d] TUN interface established [fd=%s]", connectionId, vpnInterface);
        if (isTunInterfaceValid(vpnInterface)) {
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
        } else {
            throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR);
        }
    }

    private boolean runTunReadLoop(byte[] byteBuffer) throws InterruptedException, WebSocketAlreadyShutdownException {
        ParcelFileDescriptor tunInterface = vpnInterface;
        if (tunInterface == null) {
            return false;
        }
        try (FileInputStream inputStream = new FileInputStream(tunInterface.getFileDescriptor())) {
            while (!currentThread.isInterrupted()) {
                int length = inputStream.read(byteBuffer);

                if (domainBlocker != null && DomainBlocker.isDnsPacket(byteBuffer, length)) {
                    byte[] blockedResponse = domainBlocker.processPacket(byteBuffer, length);
                    if (blockedResponse != null) {
                        if (outputStream != null) {
                            outputStream.write(blockedResponse);
                        }
                        continue;
                    }
                }

                uploadRate.update(length);
                totalUploadBytes.addAndGet(length);
                // If WebSocket isn't started (reconnecting), send() silently drops the packet.
                // No blocking/waiting on the native layer's reconnect state; the read loop
                // keeps draining the TUN interface regardless.
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
            addAlwaysExcludedApps(builder);
        } else if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
            if (appInfos.isEmpty()) {
                // No apps selected: disallow every installed app so no user traffic is tunneled
                for (ApplicationInfo appInfo : service.getPackageManager().getInstalledApplications(0)) {
                    try {
                        builder.addDisallowedApplication(appInfo.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, appInfo.packageName);
                    }
                }
            } else {
                // Implicitly tunnel Google's companion services so Google apps
                // (YouTube, Gmail, ...) don't break in allowed-only mode.
                for (String googlePackage : GOOGLE_SERVICE_PACKAGES) {
                    try {
                        builder.addAllowedApplication(googlePackage);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).d("[id=%d] Google service not installed, skipping [pkg=%s]", connectionId, googlePackage);
                    }
                }
                AppExclusion exclusion = new AppExclusion(service);
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    if (!serverEntity.isCensured() && exclusion.isExcluded(packageName)) {
                        continue;
                    }
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
            addAlwaysExcludedApps(builder);
        }
    }

    private void addAlwaysExcludedApps(VpnService.Builder builder) {
        if (serverEntity.isCensured()) {
            return;
        }
        AppExclusion exclusion = new AppExclusion(service);
        for (ApplicationInfo appInfo : service.getPackageManager().getInstalledApplications(0)) {
            if (!exclusion.isExcluded(appInfo.packageName)) {
                continue;
            }
            try {
                builder.addDisallowedApplication(appInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, appInfo.packageName);
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
        synchronized (service) {
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    XLog.tag(TAG).d("Unable to close interface", e);
                }
                vpnInterface = null;
            }
        }
        webSocketClient.shutdown();
        scheduler.shutdown();

        sendConnectionStateToService(ConnectionState.DISCONNECTED, pendingShutdownReason);
    }

    private void configureConnectionTimeSpeedScheduler() {
        try {
            connectionTime = Instant.now();
            scheduler.scheduleWithFixedDelay(() -> {
                String downloadSpeed = downloadRate.getFormatString();
                String uploadSpeed = uploadRate.getFormatString();
                long durationInSeconds = (int) Duration.between(connectionTime, Instant.now()).getSeconds();
                sendSpeedInfoAndDurationToService(downloadSpeed, uploadSpeed, durationInSeconds,
                        totalDownloadBytes.get(), totalUploadBytes.get(),
                        downloadRate.getRateForSecond(), uploadRate.getRateForSecond());
            }, 1, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            XLog.tag(TAG).i("update speed task rejected by scheduler", e);
        }
    }

    private void configureAddressesAndRoutes(VpnService.Builder builder) throws UnknownHostException, PVNClientException {
        final DnsServers dnsServers = webSocketClient.getDnsServers();
        service.cacheDnsServers(serverEntity.getId(), dnsServers);

        // Custom DNS (added first so it takes priority)
        if (customDnsIpv4 != null && !customDnsIpv4.isEmpty()) {
            XLog.tag(TAG).i("[id=%d] Custom DNS configured [ipv4=%s]", connectionId, customDnsIpv4);
            builder.addDnsServer(customDnsIpv4);
            builder.addRoute(customDnsIpv4, IP_V4_PREFIX_LENGTH);
        }

        // IPv4
        builder.addDnsServer(dnsServers.getIpv4());
        builder.addAddress(TUN_ADDRESS.getIpV4Address(), IP_V4_PREFIX_LENGTH);
        builder.addRoute(dnsServers.getIpv4(), IP_V4_PREFIX_LENGTH);

        // IPv6
        String ipv6Dns = dnsServers.getIpv6();
        if (ipv6Dns != null && !ipv6Dns.trim().isEmpty()) {
            builder.addDnsServer(ipv6Dns);
            builder.addAddress(TUN_ADDRESS.getIpV6Address(), IP_V6_PREFIX_LENGTH);
            builder.addRoute(ipv6Dns, IP_V6_PREFIX_LENGTH);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            List<IPAddress> subnetsToExcludeV4 = Stream.of(
                            LOCAL_TUN_INTERFACE_SUBNET.getAsIpV4PrefixAsString(),
                            FPTN_SERVER_SUBNET.getAsIpV4PrefixAsString(),
                            LOCAL_SUBNET.getAsIpV4PrefixAsString()
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
        if (!isTunInterfaceValid(vpnInterface)) {
            XLog.tag(TAG).i("[id=%d] WebSocket opened but TUN is gone — VPN revoked, disconnecting", connectionId);
            webSocketClient.stopWebSocket();
            service.disconnectSilently(connectionId);
            return;
        }
        reconnectCount.set(0);
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            cancelReconnectTask();
        }
    }

    private void onMessageReceived(byte[][] packets) {
        int written = 0;
        try {
            if (outputStream != null) {
                for (byte[] data : packets) {
                    if (data == null) {
                        continue;
                    }
                    outputStream.write(data);
                    written += data.length;
                }
                // Counted once per batch: both are hot on the receive path.
                downloadRate.update(written);
                totalDownloadBytes.addAndGet(written);
            }
        } catch (Exception e) {
            XLog.tag(TAG).w("[id=%d] Failed to write packet batch to TUN after %d bytes: %s",
                    connectionId, written, e.getMessage());
            if (!tunNeedsRecreate && !currentThread.isInterrupted()) {
                tunNeedsRecreate = true;
                if (vpnInterface != null) {
                    try { vpnInterface.close(); } catch (IOException ignored) {}
                }
            }
        }
    }

    public void onConnectionFailure() {
        boolean tunValid = isTunInterfaceValid(vpnInterface);
        XLog.tag(TAG).w("[id=%d] Connection failure detected [wsStarted=%b, tunValid=%b, reconnectActive=%b]",
                connectionId,
                webSocketClient.isStarted(),
                tunValid,
                onFailureScheduledTask != null && !onFailureScheduledTask.isCancelled());
        if (!tunValid) {
            XLog.tag(TAG).i("[id=%d] Disconnecting silently [tunValid=%b]", connectionId, tunValid);
            service.disconnectSilently(connectionId);
            return;
        }
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
                    cancelReconnectTask();
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
        service.sendExceptionToService(exception, connectionId);
    }

    private void sendSpeedInfoAndDurationToService(String downloadSpeed, String uploadSpeed, long duration, long totalDownload, long totalUpload, long downloadBps, long uploadBps) {
        service.updateSpeedInfo(downloadSpeed, uploadSpeed, duration, totalDownload, totalUpload, downloadBps, uploadBps);
    }

    private void sendConnectionStateToService(ConnectionState connectionState) {
        service.updateConnectionState(connectionState, reconnectCount.get(), connectionId, null);
    }

    private void sendConnectionStateToService(ConnectionState connectionState, String disconnectReason) {
        service.updateConnectionState(connectionState, reconnectCount.get(), connectionId, disconnectReason);
    }

    private void sendConnectionStateToService(ConnectionState connectionState, int count) {
        service.updateConnectionState(connectionState, count, connectionId, null);
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface != null && vpnInterface.getFileDescriptor() != null && vpnInterface.getFileDescriptor().valid();
    }

    private void protectSocket(int fd) {
        if (!service.protect(fd)) {
            XLog.tag(TAG).w("[id=%d] VpnService.protect failed [fd=%d]", connectionId, fd);
        }
    }

    public void onNetworkChanged() {
        XLog.tag(TAG).i("[id=%d] Network changed — forcing clean reconnect [wsStarted=%b]",
                connectionId, webSocketClient.isStarted());
        reconnectCount.set(0);
        onConnectionFailure();
    }
}
