package org.fptn.vpn.services;

import static org.fptn.vpn.enums.ConnectionSubnets.ALL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.FPTN_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.HZ_WHAT_IS_THIS_IP;
import static org.fptn.vpn.enums.ConnectionSubnets.LOCAL_SUBNET;
import static org.fptn.vpn.enums.ConnectionSubnets.TUN_ADDRESS;
import static org.fptn.vpn.enums.ConnectionSubnets.TUN_INTERFACE_SUBNET;

import android.app.PendingIntent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.HandlerMessageTypes;
import org.fptn.vpn.services.websocket.WebSocketAlreadyShutdownException;
import org.fptn.vpn.services.websocket.WebSocketClientWrapper;
import org.fptn.vpn.utils.DataRateCalculator;
import org.fptn.vpn.utils.IPUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import kotlin.Triple;
import lombok.Getter;
import lombok.Setter;

public class CustomVpnConnection extends Thread {
    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 1500;
    private static final int MAX_RECONNECT_COUNT = 5;
    private static final long DELAY_BETWEEN_RECONNECT_ON_FAILURE = 2L;

    @Getter
    private final int connectionId;
    private final CustomVpnService service;

    @Getter
    private final FptnServerDto fptnServerDto;
    private final WebSocketClientWrapper webSocketClient;
    @Getter
    private final String currentActiveNetworkIP;

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

    @Getter
    private Instant connectionTime;
    private ScheduledFuture<?> onFailureScheduledTask;

    public CustomVpnConnection(final CustomVpnService service,
                               final int connectionId,
                               final FptnServerDto fptnServerDto,
                               final String sniHostName,
                               final String currentActiveNetworkIP) throws PVNClientException {
        this.service = service;
        this.connectionId = connectionId;
        this.fptnServerDto = fptnServerDto;
        this.currentActiveNetworkIP = currentActiveNetworkIP;
        this.webSocketClient = new WebSocketClientWrapper(this.fptnServerDto,
                TUN_ADDRESS.getIpAddress(),
                sniHostName,
                this::onConnectionOpen,
                this::onMessageReceived,
                this::onConnectionFailure
        );
    }

    @Override
    public void run() {
        Log.d(getTag(), "CustomVpnConnection.run() Thread.id: " + Thread.currentThread().getId());
        vpnInterface = null;
        try {
            sendConnectionStateToService(ConnectionState.CONNECTING);

            //todo: extract all magic constants to class
            VpnService.Builder builder = service.new Builder();
            builder.addAddress(TUN_ADDRESS.getIpAddress(), TUN_ADDRESS.getPrefix());
            builder.addRoute(HZ_WHAT_IS_THIS_IP.getIpAddress(), HZ_WHAT_IS_THIS_IP.getPrefix());
            builder.setMtu(MAX_PACKET_SIZE);

            final String dnsServer = webSocketClient.getDnsServerIPv4();
            builder.addDnsServer(dnsServer);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.excludeRoute(new IpPrefix(InetAddress.getByName(fptnServerDto.host), 32));
                builder.excludeRoute(TUN_INTERFACE_SUBNET.getAsIpPrefix());
                builder.excludeRoute(FPTN_SUBNET.getAsIpPrefix());
                builder.excludeRoute(LOCAL_SUBNET.getAsIpPrefix());
                builder.addRoute(ALL_SUBNET.getIpAddress(), ALL_SUBNET.getPrefix());
            } else {
                IPAddress rootSubnet = new IPAddressString(ALL_SUBNET.getAsIPWithPrefix()).getAddress();
                List<IPAddress> subnetsToExclude = Stream.of(
                                fptnServerDto.host + "/32",
                                TUN_INTERFACE_SUBNET.getAsIPWithPrefix(),
                                FPTN_SUBNET.getAsIPWithPrefix(),
                                LOCAL_SUBNET.getAsIPWithPrefix()
                        )
                        .map(sub -> new IPAddressString(sub).getAddress())
                        .collect(Collectors.toList());

                List<IPAddress> subnetsToInclude = new ArrayList<>();
                IPUtils.exclude(rootSubnet, subnetsToExclude, subnetsToInclude);
                for (IPAddress ipAddress : subnetsToInclude) {
                    String hostIp = ipAddress.getLower().toAddressString().getHostAddress().toString();
                    Integer networkPrefixLength = ipAddress.getLower().toAddressString().getNetworkPrefixLength();
                    Log.d(getTag(), "subnetsToInclude.ipAddress: " + hostIp + "/" + networkPrefixLength);
                    builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : 32);
                }
            }

            builder.setSession(fptnServerDto.name).setConfigureIntent(configureVpnIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR);
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            // Packets received need to be written to this output stream.
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
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
                Log.w(getTag(), "update speed task rejected by scheduler", e);
            }

            // Packets to be sent are queued in this input stream.
            try (FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor())) {
                byte[] byteBuffer = new byte[MAX_PACKET_SIZE];
                while (!currentThread.isInterrupted()) {
                    try {
                        int length = inputStream.read(byteBuffer);
                        if (length > 0) {
                            uploadRate.update(length);
                            webSocketClient.send(Arrays.copyOf(byteBuffer, length));
                        }
                    } catch (Exception e) {
                        Log.d(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                    }
                }
            }
        } catch (PVNClientException e) {
            sendExceptionToService(e);
        } catch (IOException ex) {
            sendExceptionToService(new PVNClientException(ex.getMessage()));
        } catch (WebSocketAlreadyShutdownException e) {
            Log.w(getTag(), "The websocket already shutdown", e);
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
                Log.e(getTag(), "Unable to close interface", e);
            }
        }
        webSocketClient.shutdown();
        scheduler.shutdown();

        sendConnectionStateToService(ConnectionState.DISCONNECTED);
    }

    private void onConnectionOpen() {
        Log.d(getTag(), "onConnectionOpen()");
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            cancelReconnectTask();
            reconnectCount.set(0);
        }
    }

    private void onMessageReceived(byte[] data) {
        try {
            if (outputStream != null) {
                downloadRate.update(data.length);
                outputStream.write(data);
            }
        } catch (Exception e) {
            Log.w(getTag(), "Exception on writing to TUN interface: " + new String(data));
        }
    }

    public void onConnectionFailure() {
        Log.d(getTag(), "onConnectionFailure() Thread.id: " + Thread.currentThread().getId());
        cancelReconnectTask();
        try {
            onFailureScheduledTask = scheduler.scheduleWithFixedDelay(() -> {
                int currentCount = reconnectCount.incrementAndGet();
                Log.i(getTag(), "Reconnect WebSocket... currentCount: " + currentCount);
                if (!currentThread.isInterrupted() && isTunInterfaceValid(vpnInterface) && currentCount <= MAX_RECONNECT_COUNT) {
                    try {
                        sendConnectionStateToService(ConnectionState.RECONNECTING);
                        Log.d(getTag(), "onConnectionFailure() scheduler task Thread.id: " + Thread.currentThread().getId());
                        webSocketClient.startWebSocket();
                    } catch (PVNClientException e) {
                        if (currentCount == MAX_RECONNECT_COUNT) {
                            sendExceptionToService(e);
                            onFailureInterrupt();
                        }
                    } catch (WebSocketAlreadyShutdownException e) {
                        Log.w(getTag(), "The websocket already shutdown", e);
                        onFailureInterrupt();
                    }
                } else {
                    onFailureInterrupt();
                }
            }, 0L, DELAY_BETWEEN_RECONNECT_ON_FAILURE, TimeUnit.SECONDS);
        } catch (RejectedExecutionException exception) {
            Log.w(getTag(), "OnFailure task rejected!", exception);
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
        service.getHandler().sendMessage(Message.obtain(service.getHandler(), HandlerMessageTypes.ERROR.getValue(), connectionId, 0, exception));
    }

    private void sendSpeedInfoAndDurationToService(String downloadSpeed, String uploadSpeed, long duration) {
        service.getHandler().sendMessage(
                Message.obtain(service.getHandler(), HandlerMessageTypes.SPEED_INFO.getValue(), connectionId, 0,
                        new Triple<>(downloadSpeed, uploadSpeed, duration))
        );
    }

    private void sendConnectionStateToService(ConnectionState connectionState) {
        service.getHandler().sendMessage(
                Message.obtain(service.getHandler(), HandlerMessageTypes.CONNECTION_STATE.getValue(), connectionId, reconnectCount.get(), connectionState)
        );
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface.getFileDescriptor() != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
