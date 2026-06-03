package org.fptn.vpn.services.vpn;

import static org.fptn.vpn.enums.ConnectionSubnets.ALL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.FPTN_SERVER_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.IP_V4_PREFIX_LENGTH;
import static org.fptn.vpn.enums.ConnectionSubnets.IP_V6_PREFIX_LENGTH;
import static org.fptn.vpn.enums.ConnectionSubnets.LOCAL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.LOCAL_TUN_INTERFACE_SUBNET;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
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
import org.fptn.vpn.utils.IPUtils;
import org.fptn.vpn.views.perappvpn.AppInfo;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
     * Minimum interval between sends
     */
    public static final long MIN_SEND_INTERVAL_MS = 2;
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
    private final SniSpoofingMode sniSpoofingMode;

    @Setter
    private PendingIntent configureVpnIntent;
    // volatile: the TUN read loop runs on the connection thread while startTun() runs on the
    // main thread. Without volatile, the connection thread may cache a stale reference and
    // spin forever on the closed old fd instead of waiting for the new one.
    private volatile ParcelFileDescriptor vpnInterface;
    private volatile FileOutputStream outputStream;

    @Getter
    private Instant connectionTime;
    private ScheduledFuture<?> onFailureScheduledTask;
    private DnsServers cachedDnsServers;

    @Getter
    @Setter
    private String currentIPAddress;
    @Getter
    @Setter
    private NetworkType currentNetworkType;

    private final int maxReconnectCount;
    private final int delayBetweenAttempts;

    private final String sniHostName;
    private final BypassCensorshipMethod censorshipStrategy;
    private final PerAppVpnMode perAppVpnMode;
    private final List<AppInfo> appInfos;

    public FptnConnection(final FptnService service,
                          final int connectionId,
                          final ServerEntity serverEntity,
                          final String currentIPAddress,
                          final NetworkType currentNetworkType,
                          final int maxReconnectCount,
                          final int delayBetweenAttempts,
                          final String sniHostName,
                          final BypassCensorshipMethod censorshipStrategy,
                          final SniSpoofingMode sniSpoofingMode,
                          final PerAppVpnMode perAppVpnMode,
                          final List<AppInfo> appInfos) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverEntity = serverEntity;
        this.currentIPAddress = currentIPAddress;
        this.currentNetworkType = currentNetworkType;
        this.sniHostName = sniHostName;
        this.censorshipStrategy = censorshipStrategy;
        this.sniSpoofingMode = sniSpoofingMode;
        this.perAppVpnMode = perAppVpnMode;
        this.appInfos = appInfos;

        this.webSocketClient = new WebSocketClientWrapper(
                this.serverEntity,
                this::onConnectionOpen,
                this::onMessageReceived,
                this::onConnectionFailure,
                this.sniHostName,
                this.censorshipStrategy,
                this.sniSpoofingMode
        );

        this.maxReconnectCount = maxReconnectCount;
        this.delayBetweenAttempts = delayBetweenAttempts;
    }

    @Override
    public void run() {
        XLog.tag(TAG).i("Connection started [id=%d, server=%s, thread=%d]",
                connectionId, serverEntity.getServerInfo(), Thread.currentThread().getId());
        vpnInterface = null;
        try {
            sendConnectionStateToService(ConnectionState.CONNECTING);
            webSocketClient.startWebSocket();
            connectionTime = Instant.now();
            try {
                scheduler.scheduleWithFixedDelay(() -> {
                    // Get download and upload speeds
                    String downloadSpeed = downloadRate.getFormatString();
                    String uploadSpeed = uploadRate.getFormatString();
                    long durationInSeconds = (int) Duration.between(connectionTime, Instant.now()).getSeconds();

                    sendSpeedInfoAndDurationToService(downloadSpeed, uploadSpeed, durationInSeconds);
                }, 1, 1, TimeUnit.SECONDS); // Start after 1 second, repeat every 1 second
            } catch (RejectedExecutionException e) {
                XLog.tag(TAG).w("update speed task rejected by scheduler", e);
            }

            // Read packets
            while (!currentThread.isInterrupted()) {
                try {
                    while (!currentThread.isInterrupted() && vpnInterface == null) {
                        Thread.sleep(100);
                    }
                    if (currentThread.isInterrupted()) {
                        break;
                    }
                    try (FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor())) {
                        byte[] byteBuffer = new byte[MAX_PACKET_SIZE];
                        while (!currentThread.isInterrupted() && vpnInterface != null && vpnInterface.getFileDescriptor().valid()) {
                            try {
                                int length = inputStream.read(byteBuffer);
                                if (length > 0) {
                                    uploadRate.update(length);
                                    webSocketClient.send(byteBuffer, length);
                                } else {
                                    Thread.sleep(MIN_SEND_INTERVAL_MS);
                                }
                            } catch (Exception e) {
                                // Expected during TUN rebuild: startTun() closes the old fd on the
                                // main thread while we are blocked in read() here. With vpnInterface
                                // now volatile the outer loop will correctly wait for the new fd.
                                XLog.tag(TAG).d("[id=%d] TUN fd closed during reconnect [wsStarted=%b]: %s",
                                        connectionId, webSocketClient.isStarted(), e.getMessage());
                                break;
                            }
                        }
                    } catch (IOException e) {
                        XLog.tag(TAG).w("[id=%d] TUN interface closed — waiting for reconnect: %s",
                                connectionId, e.getMessage());
                    }
                } catch (NullPointerException e) {
                    XLog.tag(TAG).e("[id=%d] NullPointerException in TUN read loop", connectionId);
                }
            }
        } catch (PVNClientException e) {
            sendExceptionToService(e);
        } catch (WebSocketAlreadyShutdownException e) {
            XLog.tag(TAG).w("The websocket already shutdown", e);
        } catch (InterruptedException e) {
            shutdown();
        } finally {
            shutdown();
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

    /**
     * Called when the physical network changes (WiFi ↔ Cellular).
     * Unlike onConnectionFailure(), this method unconditionally stops the current WebSocket
     * before triggering a reconnect so that:
     * 1. The new WebSocket is established on the new physical network.
     * 2. onConnectionOpen() fires, which rebuilds the TUN with the new server-assigned IP.
     *    (The server assigns a fresh IP on every connect, so a stale TUN would silently
     *    black-hole traffic even though the UI shows "CONNECTED".)
     */
    public void onNetworkChanged() {
        XLog.tag(TAG).i("[id=%d] Network changed — forcing clean reconnect [wsStarted=%b]",
                connectionId, webSocketClient.isStarted());
        webSocketClient.stopWebSocket();
        onConnectionFailure();
    }

    private void startTun(String assignedIPv4, String assignedIPv6, DnsServers dns_server) {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }

            XLog.tag(TAG).i("[id=%d] Starting TUN [ipv4=%s, ipv6=%s, dns4=%s, dns6=%s, perAppMode=%s]",
                    connectionId, assignedIPv4, assignedIPv6,
                    dns_server.getIpv4(), dns_server.getIpv6(), perAppVpnMode);
            InetAddress serverInetAddress = InetAddress.getByName(serverEntity.getHost());

            VpnService.Builder builder = service.new Builder();
            builder.setBlocking(true)
                    .setSession(serverEntity.getName())
                    .setConfigureIntent(configureVpnIntent)
                    .setMtu(MAX_PACKET_SIZE);
            if (perAppVpnMode == PerAppVpnMode.OFF) {
                builder.addDisallowedApplication(service.getPackageName()); // todo: something wrong with routings
            } else if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED && !appInfos.isEmpty()) {
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    try {
                        builder.addAllowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, packageName);
                    }
                }
            } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED && !appInfos.isEmpty()) {
                builder.addDisallowedApplication(service.getPackageName());
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    try {
                        builder.addDisallowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("[id=%d] Package not found, skipping [pkg=%s]", connectionId, packageName);
                    }
                }
            }

            // IPv4
            builder.addDnsServer(dns_server.getIpv4());
            builder.addAddress(assignedIPv4, IP_V4_PREFIX_LENGTH);
            builder.addRoute(dns_server.getIpv4(), IP_V4_PREFIX_LENGTH);

            // IPv6
            builder.addDnsServer(dns_server.getIpv6());
            builder.addAddress(assignedIPv6, IP_V6_PREFIX_LENGTH);
            builder.addRoute(dns_server.getIpv6(), IP_V6_PREFIX_LENGTH);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.excludeRoute(new IpPrefix(serverInetAddress, IP_V4_PREFIX_LENGTH));
                builder.excludeRoute(LOCAL_TUN_INTERFACE_SUBNET.getAsIpV4Prefix());
                builder.excludeRoute(LOCAL_TUN_INTERFACE_SUBNET.getAsIpV6Prefix());
                builder.excludeRoute(FPTN_SERVER_SUBNET.getAsIpV4Prefix());
                builder.excludeRoute(FPTN_SERVER_SUBNET.getAsIpV6Prefix());
                builder.excludeRoute(LOCAL_SUBNET.getAsIpV4Prefix());
                builder.excludeRoute(LOCAL_SUBNET.getAsIpV6Prefix());
                builder.addRoute(ALL_SUBNET.getIpV4Address(), ALL_SUBNET.getV4prefix());
                builder.addRoute(ALL_SUBNET.getIpV6Address(), ALL_SUBNET.getV6prefix());
            } else {
                // for IPv4
                IPAddress rootSubnetV4 = new IPAddressString(ALL_SUBNET.getAsIpV4PrefixAsString()).getAddress();
                List<IPAddress> subnetsToExcludeV4 = Stream.of(
                                String.format("%s/%s", serverEntity.getHost(), IP_V4_PREFIX_LENGTH),
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

                // for IPv6
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

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR);
            }
            XLog.tag(TAG).i("[id=%d] TUN interface established [fd=%s]", connectionId, vpnInterface);
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());

        } catch (Exception e) {
            XLog.tag(TAG).e("[id=%d] Failed to establish TUN interface: %s", connectionId, e.getMessage());
            sendExceptionToService(new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR));
        }
    }

    private void onConnectionOpen() {
        XLog.tag(TAG).i("[id=%d] WebSocket connected [reconnectCount=%d]",
                connectionId, reconnectCount.get());
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            cancelReconnectTask();
            reconnectCount.set(0);
        }
        String assignedIPv4 = webSocketClient.getIPv4Address();
        String assignedIPv6 = webSocketClient.getIPv6Address();
        DnsServers dnsServers;
        if (cachedDnsServers != null) {
            XLog.tag(TAG).d("[id=%d] Using cached DNS [%s / %s]",
                    connectionId, cachedDnsServers.getIpv4(), cachedDnsServers.getIpv6());
            dnsServers = cachedDnsServers;
        } else {
            try {
                dnsServers = webSocketClient.getDnsServers();
                cachedDnsServers = dnsServers;
            } catch (PVNClientException e) {
                XLog.tag(TAG).e("[id=%d] Failed to fetch DNS servers: %s", connectionId, e.getMessage());
                service.getMainExecutor().execute(() -> sendExceptionToService(e));
                return;
            }
        }
        service.getMainExecutor().execute(() -> this.startTun(assignedIPv4, assignedIPv6, dnsServers));
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
                    if (onFailureScheduledTask != null) {
                        onFailureScheduledTask.cancel(false);
                        onFailureScheduledTask = null;
                    }
                    return;
                }

                int currentCount = reconnectCount.incrementAndGet();
                XLog.tag(TAG).i("[id=%d] Reconnecting [attempt %d/%d]", connectionId, currentCount, maxReconnectCount);
                if (!currentThread.isInterrupted() && (vpnInterface == null || isTunInterfaceValid(vpnInterface)) && currentCount <= maxReconnectCount) {
                    try {
                        sendConnectionStateToService(ConnectionState.RECONNECTING, currentCount);
                        webSocketClient.startWebSocket();
                        if (onFailureScheduledTask != null && !onFailureScheduledTask.isCancelled()) {
                            onFailureScheduledTask.cancel(false);
                        }
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
            }, 0L, delayBetweenAttempts, TimeUnit.SECONDS);
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
        service.updateConnectionState(connectionState, reconnectCount.get());
    }

    private void sendConnectionStateToService(ConnectionState connectionState, int count) {
        service.updateConnectionState(connectionState, count);
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface != null && vpnInterface.getFileDescriptor() != null && vpnInterface.getFileDescriptor().valid();
    }

}
