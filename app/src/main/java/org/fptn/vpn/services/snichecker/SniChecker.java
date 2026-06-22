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

package org.fptn.vpn.services.snichecker;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;


public class SniChecker {
    private final String TAG = getClass().getSimpleName();
    private final ServerEntity selectedServer;
    private long nativeHandle = 0;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public SniChecker(ServerEntity selectedServer, BypassCensorshipMethod bypassCensorshipMethod, SniSpoofingMode sniSpoofingMode) {
        this.selectedServer = selectedServer;
        String strategyName = "SNI";
        if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY && sniSpoofingMode == SniSpoofingMode.SNI) {
            strategyName = "SNI";
        } else if (bypassCensorshipMethod == BypassCensorshipMethod.TLS_OBFUSCATION) {
            strategyName = "OBFUSCATION";
        } else if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
            strategyName = sniSpoofingMode.toString().replace('_', '-');
        }
        this.nativeHandle = nativeCreate(
                selectedServer.getHost(),
                selectedServer.getPort(),
                selectedServer.getMd5ServerFingerprint(),
                strategyName
        );
    }

    public boolean checkSni(String sni) {
        XLog.tag(TAG).d("Checking SNI [sni=%s]", sni);

        if (nativeHandle == 0) {
            XLog.tag(TAG).e("Cannot check SNI — native handle is null [sni=%s]", sni);
            return false;
        }
        return nativeCheckSni(nativeHandle, sni);
    }

    public void close() {
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    // Native methods
    private native long nativeCreate(String host, int port, String md5Fingerprint, String censorshipStrategy);

    private native boolean nativeCheckSni(long nativeHandle, String sni);

    private native void nativeDestroy(long nativeHandle);
}
