package org.fptn.vpn.services.websocket;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

public class WebSocketClientWrapper {
    private static final String DNS_URL = "/api/v1/dns";
    private static final String LOGIN_URL = "/api/v1/login";

    private final ServerEntity serverEntity;
    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final NativeHttpsClientImpl nativeHttpsClient;
    private final String sniHostName;
    private final BypassCensorshipMethod censorshipStrategy;
    private final SniSpoofingMode sniSpoofingMode;

    private NativeWebSocketClientImpl nativeWebSocketClient;

    @Getter
    private boolean shutdown = false;

    public WebSocketClientWrapper(ServerEntity serverEntity,
                                  OnOpenCallback onOpenCallback,
                                  OnMessageReceivedCallback onMessageReceivedCallback,
                                  OnFailureCallback onFailureCallback,
                                  String sniHostName,
                                  BypassCensorshipMethod censorshipStrategy,
                                  SniSpoofingMode sniSpoofingMode) {
        this.serverEntity = serverEntity;
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        // this is SNI spoofing
        this.sniHostName = sniHostName;
        this.censorshipStrategy = censorshipStrategy;
        this.sniSpoofingMode = sniSpoofingMode;

        this.nativeHttpsClient = new NativeHttpsClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                serverEntity.getMd5ServerFingerprint(),
                sniHostName,
                censorshipStrategy,
                sniSpoofingMode
        );
    }

    public String getIPv4Address() {
        return this.nativeWebSocketClient.getIPv4Address();
    }

    public String getIPv6Address() {
        return this.nativeWebSocketClient.getIPv6Address();
    }

    public synchronized void startWebSocket() throws PVNClientException, WebSocketAlreadyShutdownException {
        if (isShutdown()) {
            throw new WebSocketAlreadyShutdownException();
        }
        stopWebSocket();

        String accessToken = getAccessToken(); // maybe move to constructor if it not changed between connections?

        nativeWebSocketClient = new NativeWebSocketClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                accessToken,
                serverEntity.getMd5ServerFingerprint(),
                onOpenCallback,
                onMessageReceivedCallback,
                onFailureCallback,
                sniHostName,
                censorshipStrategy,
                sniSpoofingMode
        );

        XLog.d(getTag(), "startWebSocket() nativeWebSocketClient.start() Thread.id: " + Thread.currentThread().getId());
        nativeWebSocketClient.start();
    }

    public synchronized void stopWebSocket() {
        XLog.d(getTag(), "stopWebSocket()");
        if (nativeWebSocketClient != null) {
            if (nativeWebSocketClient.isStarted()) {
                XLog.d(getTag(), "stopWebSocket() nativeWebSocketClient.stop() Thread.id: " + Thread.currentThread().getId());
                nativeWebSocketClient.stop();
            }
            XLog.d(getTag(), "stopWebSocket() nativeWebSocketClient.release() Thread.id: " + Thread.currentThread().getId());
            nativeWebSocketClient.release();
            nativeWebSocketClient = null;
        }
    }

    public void send(byte[] bytes, long length) {
        if (nativeWebSocketClient != null
                && nativeWebSocketClient.isStarted()
                && length > 0) {
            nativeWebSocketClient.send(bytes, length);
        } else {
            throw new RuntimeException("nativeWebSocketClient is null or not started");
        }
    }

    public synchronized void shutdown() {
        stopWebSocket();
        shutdown = true;
    }

    private String getAccessToken() throws PVNClientException {
        LoginRequest loginRequest = new LoginRequest(
                serverEntity.getUsername(),
                serverEntity.getPassword()
        );
        String requestBody = new Gson().toJson(loginRequest);

        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            NativeResponse response = nativeHttpsClient.Post(LOGIN_URL, requestBody, 5);
            if (response != null) {
                if (response.code == 200) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body);
                        String accessToken = jsonResponse.getString("access_token");
                        XLog.i(getTag(), "Getting accessToken successful.");
                        return accessToken;
                    } catch (JSONException e) {
                        XLog.e(getTag(), "Some error occurs on parsing accessToken response: " + e);
                    }
                } else if (response.code == 401) {
                    XLog.e(getTag(), "Server return unsuccess response: " + response.errorMessage);
                    throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR);
                }
            }
            XLog.w(getTag(), "getAccessToken attempt " + attempt + " failed");
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }

    public DnsServers getDnsServers() throws PVNClientException {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            NativeResponse response = nativeHttpsClient.Get(DNS_URL, 5);
            if (response != null && response.code == 200) {
                DnsServers dnsServers = new Gson().fromJson(response.body, DnsServers.class);
                XLog.i(getTag(), "DnsServers: " + dnsServers.toString());
                return dnsServers;
            }
            XLog.w(getTag(), "getDnsServers attempt " + attempt + " failed");
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }


    private String getTag() {
        return this.getClass().getCanonicalName();
    }


}
