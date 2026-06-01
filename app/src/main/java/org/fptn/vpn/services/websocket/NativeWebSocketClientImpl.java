package org.fptn.vpn.services.websocket;


import com.elvishew.xlog.XLog;

import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.concurrent.atomic.AtomicInteger;

public class NativeWebSocketClientImpl {
    private static final String TAG = NativeWebSocketClientImpl.class.getName();

    static {
        System.loadLibrary("fptn_native_lib");
    }

    /* Only for debug */
    private static final AtomicInteger SERIAL_NUM = new AtomicInteger(555);
    private final int serialNum;

    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;

    private long nativeHandle;

    public NativeWebSocketClientImpl(
            String host,
            int port,
            String accessToken,
            String md5ServerFingerprint,
            OnOpenCallback onOpenCallback,
            OnMessageReceivedCallback onMessageReceivedCallback,
            OnFailureCallback onFailureCallback,
            String sniHostName,
            BypassCensorshipMethod censorshipStrategy,
            SniSpoofingMode sniSpoofingMode) throws PVNClientException {
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        String censorshipStrategyName = "SNI";
        if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY && sniSpoofingMode == SniSpoofingMode.SNI) {
            censorshipStrategyName = "SNI";
        } else if (censorshipStrategy == BypassCensorshipMethod.TLS_OBFUSCATION) {
            censorshipStrategyName = "OBFUSCATION";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY) {
            censorshipStrategyName = sniSpoofingMode.toString().replace('_', '-');
        }

        this.nativeHandle = nativeCreate(
                host,
                port,
                sniHostName,
                accessToken,
                md5ServerFingerprint,
                censorshipStrategyName
        );

        this.serialNum = SERIAL_NUM.getAndIncrement();

        if (this.nativeHandle == 0L) {
            throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
        }
    }

    public void start() {
        XLog.tag(TAG).d("start() [serial=%d, thread=%d]", serialNum, Thread.currentThread().getId());
        if (!nativeIsStarted(nativeHandle)) {
            nativeRun(nativeHandle);
        }
    }

    public void stop() {
        XLog.tag(TAG).d("stop() [serial=%d, thread=%d]", serialNum, Thread.currentThread().getId());
        if (nativeIsStarted(nativeHandle)) {
            nativeStop(nativeHandle);
        }
    }

    public boolean isStarted() {
        return nativeIsStarted(nativeHandle);
    }

    public void send(byte[] data, long length) {
        if (nativeHandle != 0L && nativeIsStarted(nativeHandle)) {
            nativeSend(nativeHandle, data, length);
        }
    }

    public synchronized void release() {
        XLog.tag(TAG).d("release() [serial=%d, thread=%d]", serialNum, Thread.currentThread().getId());
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        XLog.tag(TAG).d("finalize() [serial=%d, thread=%d]", serialNum, Thread.currentThread().getId());
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    public void onOpenImpl() {
        XLog.tag(TAG).i("WebSocket opened — dispatching onOpen [serial=%d]", serialNum);
        if (this.onOpenCallback != null) {
            this.onOpenCallback.onOpen();
        }
        XLog.tag(TAG).d("onOpenImpl completed [serial=%d]", serialNum);
    }

    public void onFailureImpl() {
        XLog.tag(TAG).w("WebSocket failure — dispatching onFailure [serial=%d]", serialNum);
        if (this.onFailureCallback != null) {
            this.onFailureCallback.onFailure();
        }
        XLog.tag(TAG).d("onFailureImpl completed [serial=%d]", serialNum);
    }

    public void onMessageImpl(byte[] msg) {
        if (this.onMessageReceivedCallback != null) {
            this.onMessageReceivedCallback.onMessageReceived(msg);
        }
    }

    public String getIPv4Address() {
        if (nativeHandle != 0L) {
            return nativeGetIPv4Address(nativeHandle);
        }
        return "";
    }

    public String getIPv6Address() {
        if (nativeHandle != 0L) {
            return nativeGetIPv6Address(nativeHandle);
        }
        return "";
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String sni,
                                     String access_token,
                                     String expected_md5_fingerprint,
                                     String censorship_strategy_name);

    private native void nativeDestroy(long nativeHandle);

    private native boolean nativeRun(long nativeHandle);

    private native boolean nativeStop(long nativeHandle);

    private native boolean nativeSend(long nativeHandle, byte[] data, long length);

    private native boolean nativeIsStarted(long nativeHandle);

    private native String nativeGetIPv4Address(long nativeHandle);

    private native String nativeGetIPv6Address(long nativeHandle);
}
