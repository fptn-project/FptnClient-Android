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

package org.fptn.vpn.services.tile;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.VpnService;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.utils.PermissionsUtils;

import lombok.Getter;

public class FptnTileService extends TileService {
    private static final String TAG = FptnTileService.class.getSimpleName();

    @Getter
    private static final MutableLiveData<ConnectionState> serviceStateMutableLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);

    // Written out explicitly (instead of relying on Lombok's @Getter) because Kotlin's
    // Java-interop stub generation runs before the Lombok annotation processor, so
    // Kotlin/Compose call sites can't see the Lombok-generated static getter here.
    public static MutableLiveData<ConnectionState> getServiceStateMutableLiveData() {
        return serviceStateMutableLiveData;
    }

    private Observer<ConnectionState> serviceStateObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        XLog.tag(TAG).i("Tile service created");
        serviceStateObserver = connectionState -> updateTile();
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        XLog.tag(TAG).i("Tile service destroyed");
        if (serviceStateObserver != null) {
            serviceStateMutableLiveData.removeObserver(serviceStateObserver);
            serviceStateObserver = null;
        }
    }

    @Override
    public void onClick() {

        unlockAndRun(() -> {
            XLog.tag(TAG).i("Tile clicked [state=%s]", serviceStateMutableLiveData.getValue());
            ConnectionState connectionState = serviceStateMutableLiveData.getValue();
            if (connectionState != null && connectionState.isActiveState()) {
                FptnService.startToDisconnect(this);
            } else {
                // Check notification enabled
                if (!PermissionsUtils.checkNotificationEnabled(this)) {
                    // And if not - open main activity
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                    );

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startActivityAndCollapse(pendingIntent);
                    } else {
                        startActivityAndCollapse(intent);
                    }

                    return;
                }

                Intent vpnIntent = VpnService.prepare(this);
                if (vpnIntent != null) {
                    XLog.tag(TAG).w("VPN permission not granted — launching app for user confirmation");
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (launchIntent != null) {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(
                                this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            startActivityAndCollapse(pendingIntent);
                        } else {
                            startActivityAndCollapse(launchIntent);
                        }
                    }
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    FptnService.startToConnectFromTile(this);
                } else {
                    FptnService.startToConnect(this);
                }
            }
        });
    }

    @Override
    public void onStartListening() {
        XLog.tag(TAG).i("Tile panel opened [state=%s]", serviceStateMutableLiveData.getValue());
        updateTile();
    }

    private void updateTile() {
        XLog.tag(TAG).d("Updating tile [state=%s]", serviceStateMutableLiveData.getValue());
        Tile tile = getQsTile();
        if (tile != null) {
            // Update tile state
            ConnectionState connectionState = serviceStateMutableLiveData.getValue();
            if (connectionState != null && connectionState.isActiveState()) {
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel("FPTN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("ON");
                }
                tile.setContentDescription("Connected to ESTONIA");
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_logo));
                tile.updateTile();
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel("FPTN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("OFF");
                }
                tile.setContentDescription("Disconnected");
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_logo));
                tile.updateTile();
            }
        }
    }
}
