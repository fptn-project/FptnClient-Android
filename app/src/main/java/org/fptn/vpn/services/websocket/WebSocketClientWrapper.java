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

    private final String tunAddressIPv4;
    private final String tunAddressIPv6;

    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final NativeHttpsClientImpl nativeHttpsClient;
    private final String sniHostName;
    private final BypassCensorshipMethod censorshipStrategy;
    private final SniSpoofingMode sniSpoofingMode;

    private NativeWebSocketClientImpl nativeWebSocketClient;
    private String cachedAccessToken = null;

    @Getter
    private boolean shutdown = false;

    public WebSocketClientWrapper(ServerEntity serverEntity,
                                  String tunAddressIPv4,
                                  String tunAddressIPv6,
                                  OnOpenCallback onOpenCallback,
                                  OnMessageReceivedCallback onMessageReceivedCallback,
                                  OnFailureCallback onFailureCallback,
                                  String sniHostName,
                                  BypassCensorshipMethod censorshipStrategy,
                                  SniSpoofingMode sniSpoofingMode) {
        this.serverEntity = serverEntity;
        this.tunAddressIPv4 = tunAddressIPv4;
        this.tunAddressIPv6 = tunAddressIPv6;
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

    public boolean isStarted() {
        return nativeWebSocketClient != null && nativeWebSocketClient.isStarted();
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

        if (cachedAccessToken == null) {
            cachedAccessToken = getAccessToken();
        } else {
            XLog.d(getTag(), "Re-using cached access token (skipping login)");
        }

        nativeWebSocketClient = new NativeWebSocketClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                tunAddressIPv4,
                tunAddressIPv6,
                cachedAccessToken,
                serverEntity.getMd5ServerFingerprint(),
                onOpenCallback,
                onMessageReceivedCallback,
                onFailureCallback,
                sniHostName,
                censorshipStrategy,
                sniSpoofingMode
        );

        XLog.d(getTag(), "WebSocket start dispatched [thread=" + Thread.currentThread().getId() + "]");
        nativeWebSocketClient.start();
    }

    public synchronized void stopWebSocket() {
        XLog.d(getTag(), "stopWebSocket called [thread=" + Thread.currentThread().getId() + "]");
        if (nativeWebSocketClient != null) {
            if (nativeWebSocketClient.isStarted()) {
                XLog.i(getTag(), "Stopping active WebSocket [thread=" + Thread.currentThread().getId() + "]");
                nativeWebSocketClient.stop();
            }
            XLog.i(getTag(), "Releasing WebSocket resources [thread=" + Thread.currentThread().getId() + "]");
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

    public void invalidateAccessToken() {
        cachedAccessToken = null;
    }

    private String getAccessToken() throws PVNClientException {
        LoginRequest loginRequest = new LoginRequest(
                serverEntity.getUsername(),
                serverEntity.getPassword()
        );
        String requestBody = new Gson().toJson(loginRequest);

        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            NativeResponse response = nativeHttpsClient.Post(LOGIN_URL, requestBody, 6);
            if (response != null) {
                if (response.code == 200) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body);
                        String accessToken = jsonResponse.getString("access_token");
                        XLog.tag(getTag()).i("Access token acquired successfully [attempt=%d]", attempt);
                        return accessToken;
                    } catch (JSONException e) {
                        XLog.tag(getTag()).e("Failed to parse access token response: %s", e.getMessage());
                    }
                } else if (response.code == 401) {
                    XLog.tag(getTag()).e("Authentication rejected by server [code=401, error=%s]", response.errorMessage);
                    throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR);
                }
            }
            XLog.tag(getTag()).w("Access token request failed [attempt=%d/%d]", attempt, maxAttempts);
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
            NativeResponse response = nativeHttpsClient.Get(DNS_URL, 6);
            if (response != null && response.code == 200) {
                DnsServers dnsServers = new Gson().fromJson(response.body, DnsServers.class);
                XLog.tag(getTag()).i("DNS servers received: %s", dnsServers.toString());
                return dnsServers;
            }
            XLog.tag(getTag()).w("DNS server request failed [attempt=%d/%d]", attempt, maxAttempts);
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
