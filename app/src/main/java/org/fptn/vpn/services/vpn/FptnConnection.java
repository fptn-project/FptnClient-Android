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
    private ParcelFileDescriptor vpnInterface;
    private FileOutputStream outputStream;

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
                          final List<AppInfo> appInfos) throws UnknownHostException {
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

        InetAddress inetAddress = InetAddress.getByName(serverEntity.getHost());
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
        XLog.tag(TAG).d("CustomVpnConnection.run() Thread.id: " + Thread.currentThread().getId());
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
                    XLog.tag(TAG).i("TUN interface is ready, starting packet processing");
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
                                XLog.tag(TAG).d("Error reading data from VPN interface: " + e.getMessage());
                                break;
                            }
                        }
                    } catch (IOException e) {
                        XLog.tag(TAG).e("VPN interface closed, waiting for new one", e);
                    }
                } catch (NullPointerException e) {
                    XLog.tag(TAG).e("NullPointerException");
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
        if (!currentThread.isInterrupted()) {
            currentThread.interrupt();
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

    private void startTun(String assignedIPv4, String assignedIPv6) {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }

            XLog.tag(TAG).i("Addresses: " + assignedIPv4 + " " + assignedIPv6);
            InetAddress inetAddress = InetAddress.getByName(serverEntity.getHost());
            DnsServers dns_server = webSocketClient.getDnsServers();

            VpnService.Builder builder = service.new Builder();
            builder.setBlocking(true)
                    .setSession(serverEntity.getName())
                    .setConfigureIntent(configureVpnIntent)
                    .setMtu(MAX_PACKET_SIZE);

            // From documentation: You can create either an allowed list, or, a disallowed list, but not both
            XLog.tag(TAG).i("PerAppVpnMode: " + perAppVpnMode);
            XLog.tag(TAG).i("appInfos: " + appInfos);
            if (perAppVpnMode == PerAppVpnMode.OFF) {
                builder.addDisallowedApplication(service.getPackageName());
            } else if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED && !appInfos.isEmpty()) {
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    try {
                        builder.addAllowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("Package not found: " + packageName);
                    }
                }
            } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED && !appInfos.isEmpty()) {
                builder.addDisallowedApplication(service.getPackageName());
                for (AppInfo appInfo : appInfos) {
                    String packageName = appInfo.getPackageName();
                    try {
                        builder.addDisallowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        XLog.tag(TAG).w("Package not found: " + packageName);
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
                builder.excludeRoute(new IpPrefix(inetAddress, IP_V4_PREFIX_LENGTH));
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
                    XLog.tag(TAG).d("subnetsToIncludeV4.ipAddress: " + hostIp + "/" + networkPrefixLength);
                    builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : IP_V4_PREFIX_LENGTH);
                }

                // for IPv6
                IPAddress rootSubnetV6 = new IPAddressString(ALL_SUBNET.getAsIpV6PrefixAsString()).getAddress();
                XLog.tag(TAG).i("rootSubnetV6: " + rootSubnetV6);
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
                    XLog.tag(TAG).d("subnetsToIncludeV6.ipAddress: " + hostIp + "/" + networkPrefixLength);
                    builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : IP_V6_PREFIX_LENGTH);
                }
            }

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR);
            }
            XLog.tag(TAG).i("New interface: " + vpnInterface);

            // Packets received need to be written to this output stream.
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());

        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to start TUN interface", e);
            sendExceptionToService(new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR));
        }
    }

    private void onConnectionOpen() {
        XLog.tag(TAG).d("onConnectionOpen()");
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            cancelReconnectTask();
            reconnectCount.set(0);
        }
        String assignedIPv4 = webSocketClient.getIPv4Address();
        String assignedIPv6 = webSocketClient.getIPv6Address();
        service.getMainExecutor().execute(() -> {
            XLog.tag(TAG).d("Received from server - IPv4: " + assignedIPv4);
            XLog.tag(TAG).d("Received from server - IPv6: " + assignedIPv6);
            this.startTun(assignedIPv4, assignedIPv6);
        });
    }

    private void onMessageReceived(byte[] data) {
        try {
            if (outputStream != null) {
                downloadRate.update(data.length);
                outputStream.write(data);
            }
        } catch (Exception e) {
            XLog.tag(TAG).w("Exception on writing to TUN interface: " + new String(data));
        }
    }

    public void onConnectionFailure() {
        XLog.tag(TAG).d("onConnectionFailure() Thread.id: " + Thread.currentThread().getId());
        cancelReconnectTask();
        try {
            onFailureScheduledTask = scheduler.scheduleWithFixedDelay(() -> {
                int currentCount = reconnectCount.incrementAndGet();
                XLog.tag(TAG).i("Reconnect WebSocket... currentCount: " + currentCount);
                if (!currentThread.isInterrupted() && isTunInterfaceValid(vpnInterface) && currentCount <= maxReconnectCount) {
                    try {
                        sendConnectionStateToService(ConnectionState.RECONNECTING);
                        XLog.tag(TAG).d("onConnectionFailure() scheduler task Thread.id: " + Thread.currentThread().getId());
                        webSocketClient.startWebSocket();
                    } catch (PVNClientException e) {
                        if (currentCount == maxReconnectCount) {
                            sendExceptionToService(e);
                            onFailureInterrupt();
                        }
                    } catch (WebSocketAlreadyShutdownException e) {
                        XLog.tag(TAG).w("The websocket already shutdown", e);
                        onFailureInterrupt();
                    }
                } else {
                    onFailureInterrupt();
                }
            }, 0L, delayBetweenAttempts, TimeUnit.SECONDS);
        } catch (RejectedExecutionException exception) {
            XLog.tag(TAG).w("OnFailure task rejected!", exception);
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

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface.getFileDescriptor() != null;
    }

}
