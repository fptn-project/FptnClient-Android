package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.enums.TLSHandshakeObfuscation;

public class NativeHttpsClientImpl {
    private static final String TAG = NativeHttpsClientImpl.class.getName();

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public NativeHttpsClientImpl(String server_ip,
                                 int server_port,
                                 String md5_fingerprint,
                                 String sni) {
        this.nativeHandle = nativeCreate(
                server_ip,
                server_port,
                sni,
                md5_fingerprint
        );
    }

    public NativeHttpsClientImpl(String server_ip,
                                 int server_port,
                                 String md5_fingerprint,
                                 TLSHandshakeObfuscation tlsHandshakeObfuscation) {
        // todo: need add to native new constructor with tlsHandshakeObfuscation
        // todo: or in native code check if string == one of values of tlsHandshakeObfuscation
        this.nativeHandle = nativeCreate(
                server_ip,
                server_port,
                tlsHandshakeObfuscation.toString(),
                md5_fingerprint
        );
    }

    public NativeResponse Get(String url, int timeout) {
        return nativeGet(nativeHandle, url, timeout);
    }

    public NativeResponse Post(String url, String body, int timeout) {
        return nativePost(nativeHandle, url, body, timeout);
    }

    public synchronized void release() {
        Log.d(TAG, "NativeHttpsClientImpl.release()");
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String sni,
                                     String expected_md5_fingerprint);

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "NativeHttpsClientImpl.finalize()");
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    private native void nativeDestroy(long nativeHandle);

    private native NativeResponse nativeGet(long nativeHandle, String url, int timeout);

    private native NativeResponse nativePost(long nativeHandle, String url, String requestBody, int timeout);
}
