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
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.utils.PermissionsUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.Getter;

public class FptnTileService extends TileService {
    private static final String TAG = FptnTileService.class.getSimpleName();

    // Background executor for synchronous Room reads inside onClick. The AppDatabase does not
    // allow main-thread queries, and onClick runs on the main thread — we need to bounce the
    // session check off a worker, with a strict timeout so a stuck DB query can never strand the
    // tile click (the user gets the old behavior instead).
    private static final ExecutorService DB_CHECK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final long DB_CHECK_TIMEOUT_SECONDS = 2L;

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

                // Security gate: refuse to start the VPN from the tile when the user has not
                // signed in yet (no servers AND no tokens in the DB). Previously the tile would
                // kick off FptnService regardless, which produced a "Selecting the best server..."
                // notification and then — via AppNavHost's auto-routing on the active ConnectionState —
                // dropped the user onto the Home screen without ever going through Login. Open the
                // app on the launcher Intent instead; MainActivity will start at Splash, which will
                // route to Login because the DB is empty.
                //
                // The check runs off the main thread because AppDatabase does not allow
                // main-thread queries. A 2s timeout keeps us from blocking forever if Room is
                // stuck — on timeout we cancel the future and fall through to the legacy "start
                // VPN" path; the FptnService.startConnectionAttempt + AppNavHost gates still
                // protect the UI.
                boolean sessionEstablished = false;
                FutureTask<Boolean> checkTask = new FutureTask<>(() -> {
                    AppDatabase db = AppDatabase.getInstance(FptnTileService.this);
                    return db.serverDAO().getCountSync() > 0 && db.tokenDAO().getCount() > 0;
                });
                DB_CHECK_EXECUTOR.execute(checkTask);
                try {
                    sessionEstablished = checkTask.get(DB_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    XLog.tag(TAG).w("Session check timed out — falling through to legacy tile behavior");
                    // Cancel the still-running query so it can't leak threads / hold the DB.
                    checkTask.cancel(true);
                } catch (Exception e) {
                    XLog.tag(TAG).e("Failed to check session before starting VPN from tile: %s", e.getMessage());
                    // Fall through and let VpnService.prepare / FptnService handle it as before —
                    // we'd rather start than strand the user. The AppNavHost gate still protects UI.
                }

                if (!sessionEstablished) {
                    XLog.tag(TAG).w("Refusing to start VPN from tile — session not established");
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (launchIntent != null) {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                                this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            startActivityAndCollapse(launchPendingIntent);
                        } else {
                            startActivityAndCollapse(launchIntent);
                        }
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
