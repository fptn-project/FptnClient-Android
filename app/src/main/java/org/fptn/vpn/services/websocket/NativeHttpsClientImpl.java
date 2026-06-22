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

package org.fptn.vpn.services.websocket;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;

public class NativeHttpsClientImpl {
    private static final String TAG = NativeHttpsClientImpl.class.getName();

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public NativeHttpsClientImpl(String serverIP,
                                 int serverPort,
                                 String md5Fingerprint,
                                 String sni,
                                 BypassCensorshipMethod censorshipStrategy,
                                 SniSpoofingMode sniSpoofingMode) {
        String censorshipStrategyName = "SNI";
        if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY && sniSpoofingMode == SniSpoofingMode.SNI) {
            censorshipStrategyName = "SNI";
        } else if (censorshipStrategy == BypassCensorshipMethod.TLS_OBFUSCATION) {
            censorshipStrategyName = "OBFUSCATION";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY) {
            censorshipStrategyName = sniSpoofingMode.toString().replace('_', '-');
        }

        this.nativeHandle = nativeCreate(
                serverIP,
                serverPort,
                sni,
                md5Fingerprint,
                censorshipStrategyName
        );
    }

    public NativeResponse Get(String url, int timeout) {
        return nativeGet(nativeHandle, url, timeout);
    }

    public NativeResponse Post(String url, String body, int timeout) {
        return nativePost(nativeHandle, url, body, timeout);
    }

    public synchronized void release() {
        XLog.tag(TAG).d("release() [handle=%d]", nativeHandle);
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String sni,
                                     String expected_md5_fingerprint,
                                     String censorship_strategy_name);

    @Override
    protected void finalize() throws Throwable {
        XLog.tag(TAG).d("finalize() GC triggered");
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
