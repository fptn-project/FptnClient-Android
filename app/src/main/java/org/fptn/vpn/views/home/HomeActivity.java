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

package org.fptn.vpn.views.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.vpn.FptnServiceState;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.CustomSpinner;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.utils.ViewUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.adapter.ServerEntityAdapter;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    @Getter
    private HomeActivityViewModel viewModel;

    private TextView connectionTimerTextView;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView statusTextView;

    private TextView connectedServerTextView;

    private View connectionTimeFrame;
    private View serverInfoFrame;
    private View homeTrafficFrame;
    private View permissionWarningFrame;
    private TrafficSpeedChart trafficSpeedChart;

    private CustomSpinner spinnerServers;

    private ToggleButton startStopButton;

    //for service binding
    private ServiceConnection connection;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        initializeVariable();
    }

    @Override
    protected void onStart() {
        super.onStart();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                XLog.tag(TAG).i("VPN service connected [component=%s]", name.getShortClassName());
                FptnService.LocalBinder localBinder = (FptnService.LocalBinder) service;
                viewModel.subscribeService(localBinder.getService());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                try {
                    if (viewModel != null) {
                        viewModel.unsubscribe();
                    }
                } catch (Exception e) {
                    XLog.tag(TAG).e("Error handling VPN service disconnect: %s", e.getMessage());
                }
            }
        };
        FptnService.bindService(this, connection);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (connection != null) {
                unbindService(connection);
            }
        } catch (Exception e) {
            XLog.tag(TAG).e("Error unbinding VPN service: %s", e.getMessage());
        }
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        ServerEntityAdapter serverEntityAdapter = new ServerEntityAdapter(R.layout.home_list_recycler_server_item);
        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter(serverEntityAdapter);

        startStopButton = findViewById(R.id.home_do_connect_button);
        startStopButton.setOnClickListener(this::onClickToStartStop);
        adjustButtonVerticalBias();

        /*View containers to hide*/
        homeTrafficFrame = findViewById(R.id.home_traffic_frame);
        trafficSpeedChart = findViewById(R.id.home_traffic_chart);
        connectionTimeFrame = findViewById(R.id.home_connection_timer_frame);
        serverInfoFrame = findViewById(R.id.home_server_info_frame);

        viewModel = new ViewModelProvider(this).get(HomeActivityViewModel.class);
        viewModel.getServerDtoListLiveData().observe(this, serverEntities -> {
            ((ServerEntityAdapter) spinnerServers.getAdapter()).setServerEntityList(serverEntities);

            spinnerServers.performClosedEvent(); // FIX SPINNER BACKGROUND
        });

        View settingsMenuItem = findViewById(R.id.menuSettings);
        viewModel.getServiceStateMutableLiveData().observe(this, fptnServiceState -> {
            // we can't change UI from viewModel
            switch (fptnServiceState.getConnectionState()) {
                case CONNECTED:
                    connectedStateUiItems();

                    viewModel.stopCheckingPing();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
                    updateSpinnerSelection();

                    // Check activity on foreground
                    if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                        viewModel.startCheckingPing();
                    }

                    break;
                default:
                    break;
            }

            boolean activeState = fptnServiceState.getConnectionState().isActiveState();
            startStopButton.setChecked(activeState);
            spinnerServers.setEnabled(!activeState);
            settingsMenuItem.setEnabled(!activeState);

            // we can't show Snackbar from viewModel
            PVNClientException exception = fptnServiceState.getException();
            if (exception != null) {
                if (ErrorCode.Companion.isNeedToOfferRefreshToken(exception.errorCode)) {
                    String errorText = Optional.ofNullable(viewModel.getStatusTextLiveData().getValue())
                            .orElse(ErrorCode.UNKNOWN_ERROR.getValue());
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.layout), errorText, 8000);
                    if (ErrorCode.Companion.isNeedToOfferRefreshToken(exception.errorCode)) {
                        snackbar.setAction(getString(R.string.refresh_token), v -> {
                            Intent browserIntent = new
                                    Intent(Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.telegram_bot_link)));
                            startActivity(browserIntent);
                        });
                    }
                    snackbar.show();
                }
            }
        });


        downloadTextView = findViewById(R.id.home_download_speed);
        viewModel.getDownloadSpeedAsStringLiveData().observe(this, downloadSpeed -> downloadTextView.setText(downloadSpeed));

        uploadTextView = findViewById(R.id.home_upload_speed);
        viewModel.getUploadSpeedAsStringLiveData().observe(this, uploadSpeed -> uploadTextView.setText(uploadSpeed));

        TextView downloadTrafficTextView = findViewById(R.id.home_download_traffic);
        viewModel.getDownloadTrafficLiveData().observe(this, t -> downloadTrafficTextView.setText(t));

        TextView uploadTrafficTextView = findViewById(R.id.home_upload_traffic);
        viewModel.getUploadTrafficLiveData().observe(this, t -> uploadTrafficTextView.setText(t));

        viewModel.getSpeedSampleLiveData().observe(this, bps -> {
            if (trafficSpeedChart != null && bps != null) {
                trafficSpeedChart.addSample(bps[0], bps[1]);
            }
        });

        connectionTimerTextView = findViewById(R.id.home_connection_timer);
        viewModel.getTimerTextLiveData().observe(this, timerText -> connectionTimerTextView.setText(timerText));

        statusTextView = findViewById(R.id.home_connection_status);
        viewModel.getStatusTextLiveData().observe(this, statusText -> statusTextView.setText(statusText));

        connectedServerTextView = findViewById(R.id.home_connected_server_name);
        viewModel.getConnectedServerInfoLiveData().observe(this, serverInfoText -> connectedServerTextView.setText(serverInfoText));

        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuHome);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuHome));

        permissionWarningFrame = findViewById(R.id.home_permission_warning_frame);

        // hide
        disconnectedStateUiItems();

        requestAddTileService();
    }

    private void adjustButtonVerticalBias() {
        int screenHeightDp = (int) (getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density);
        float bias = screenHeightDp < 600 ? 0.10f : screenHeightDp < 700 ? 0.15f : 0.25f;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) startStopButton.getLayoutParams();
        params.verticalBias = bias;
        startStopButton.setLayoutParams(params);
    }

    private void updateSpinnerSelection() {
        if (spinnerServers.getAdapter() != null
                && spinnerServers.getAdapter() instanceof ServerEntityAdapter serverEntityAdapter) {
            List<ServerEntity> serverEntityList = serverEntityAdapter.getServerEntityList();
            if (serverEntityList != null && !serverEntityList.isEmpty()) {
                if (SharedPrefUtils.getResetSelectedServerEnabled(this)) {
                    spinnerServers.setSelection(0);
                } else {
                    boolean selected = false;
                    for (int i = 0; i < serverEntityList.size(); i++) {
                        if (serverEntityList.get(i).isSelected()) {
                            spinnerServers.setSelection(i);
                            selected = true;
                            break;
                        }
                    }
                    if (!selected) {
                        spinnerServers.setSelection(0);
                    }
                }
            }
        }
    }

    // Activity on background
    @Override
    protected void onPause() {
        super.onPause();

        XLog.tag(TAG).d("Activity paused — ping checks suspended");

        viewModel.stopCheckingPing();
    }

    // Activity on foreground
    @Override
    protected void onResume() {
        super.onResume();

        XLog.tag(TAG).d("Activity resumed");

        if (!isFinishing() && !isDestroyed()) {
            bottomNavigationView.setSelectedItemId(R.id.menuHome);
        }

        Optional.ofNullable(viewModel.getServiceStateMutableLiveData())
                .map(LiveData::getValue)
                .map(FptnServiceState::getConnectionState)
                .ifPresent((state) -> {
                    if (state == ConnectionState.DISCONNECTED) {
                        viewModel.startCheckingPing();
                    }
                });
    }

    private void disconnectedStateUiItems() {
        ViewUtils.hideView(connectionTimeFrame);
        ViewUtils.hideView(serverInfoFrame);
        ViewUtils.hideView(homeTrafficFrame);
        ViewUtils.hideView(permissionWarningFrame);
        if (trafficSpeedChart != null) trafficSpeedChart.reset();

        ViewUtils.showView(spinnerServers);

        if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
            viewModel.startCheckingPing();
        } else {
            viewModel.stopCheckingPing();
        }
    }

    private void connectedStateUiItems() {
        ViewUtils.showView(connectionTimeFrame);
        ViewUtils.showView(serverInfoFrame);
        ViewUtils.showView(homeTrafficFrame);

        // check is need to show permissions warning
        if (!PermissionsUtils.isAllOptionalPermissionsGranted(this)) {
            ViewUtils.showView(permissionWarningFrame);
        }

        ViewUtils.hideView(spinnerServers);
    }

    public void onClickToStartStop(View v) {
        ConnectionState currentConnectionState = Optional.ofNullable(viewModel.getServiceStateMutableLiveData().getValue())
                .map(FptnServiceState::getConnectionState)
                .orElse(ConnectionState.DISCONNECTED);
        if (currentConnectionState == ConnectionState.DISCONNECTED) {

            // Check notification enabled
            if (!PermissionsUtils.checkNotificationEnabled(this)) {
                Toast.makeText(this, R.string.notifications_request_title, Toast.LENGTH_SHORT)
                        .show();

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                startStopButton.setChecked(false);
                return;
            }

            // Request required permission
            boolean hasPermissionsRequestedBefore = SharedPrefUtils.isPermissionsRequested(this);
            if (!hasPermissionsRequestedBefore) {
                // we don't know result of vpn permission request yet
                startStopButton.setChecked(false);

                requestRequiredPermissions();

                // remember to not ask everytime
                SharedPrefUtils.savePermissionsRequested(this, true);

                // we call onClick later - when receive all permissions request results
                return;
            }

            if (PermissionsUtils.isAlwaysOnVpnEnabledByAnotherApp(this)) {
                startStopButton.setChecked(false);
                showVpnSetupErrorDialog();
                return;
            }

            Intent intent = VpnService.prepare(this);
            if (intent != null) {
                // Request to user on launch vpn
                vpnPermissionActivityResultLauncher.launch(intent);
                // we don't know result of vpn permission request yet
                startStopButton.setChecked(false);
            } else {
                // explicit assignment cause service may start slowly
                viewModel.getServiceStateMutableLiveData().postValue(FptnServiceState.FAKE_CONNECTING);

                FptnService.startToConnect(this, (ServerEntity) spinnerServers.getSelectedItem());
            }
        } else {
            if (currentConnectionState.isActiveState()) {
                FptnService.startToDisconnect(this);
            }
        }
    }

    private void showVpnSetupErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.vpn_setup_error_title)
                .setMessage(R.string.vpn_setup_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.open_vpn_settings, (d, w) ->
                        startActivity(new Intent(Settings.ACTION_VPN_SETTINGS)))
                .show();
    }

    private void requestAddTileService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!SharedPrefUtils.isQuickSettingsTileRequested(this)) {
                @SuppressLint("WrongConstant") StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                try {
                    // Request to add a custom tile service
                    statusBarManager.requestAddTileService(
                            new ComponentName(this, FptnTileService.class),
                            "FPTN",
                            Icon.createWithResource(this, R.drawable.ic_logo),
                            this.getMainExecutor(),
                            (resultCode) -> {
                                if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    XLog.tag(TAG).i("Quick settings tile already present — no action needed");
                                    Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT)
                                            .show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    XLog.tag(TAG).i("Quick settings tile added successfully");
                                    Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    XLog.tag(TAG).i("User declined quick settings tile prompt [resultCode=%d]", resultCode);
                                }
                            }
                    );
                } catch (Exception e) {
                    XLog.tag(TAG).e("Failed to request quick settings tile addition: %s", e.getMessage());
                    Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT)
                            .show();
                }

                SharedPrefUtils.saveQuickSettingsTileRequested(this, true);
            }
        }
    }

    private final ActivityResultLauncher<Intent> vpnPermissionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), activityResult -> {
                if (activityResult != null && activityResult.getResultCode() == RESULT_OK) {
                    FptnService.startToConnect(this, (ServerEntity) spinnerServers.getSelectedItem());
                } else {
                    Toast.makeText(this, R.string.vpn_permission_warning, Toast.LENGTH_SHORT).show();
                    viewModel.getErrorTextLiveData().postValue(getString(R.string.vpn_permission_warning));
                }
            }
    );

    private final AtomicInteger requestedPermissions = new AtomicInteger(0);

    private final ActivityResultLauncher<Intent> settingsPermissionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            activityResult -> {
                if (activityResult != null && activityResult.getResultCode() == RESULT_OK) {
                    XLog.tag(TAG).i("System permission granted via Settings");
                } else {
                    XLog.tag(TAG).w("System permission denied via Settings");
                }
                if (requestedPermissions.decrementAndGet() == 0) {
                    startStopButton.callOnClick();
                }
            }
    );

    /* PERMISSIONS PART */
    @SuppressLint("BatteryLife")
    private void requestRequiredPermissions() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_request_title))
                .setMessage(getString(R.string.permission_request_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    // Battery optimization permission
                    if (!PermissionsUtils.checkBatteryOptimizations(this)) {
                        //Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        requestedPermissions.incrementAndGet();
                        startActivityWithSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    }
                    // Background data transfer restriction permission
                    if (!PermissionsUtils.checkBackgroundDataTransferRestrictions(this)) {
                        requestedPermissions.incrementAndGet();
                        startActivityWithSettings(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                    }
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    XLog.tag(TAG).w("Optional permissions denied by user — continuing without them");
                    // it must work without permissions
                    startStopButton.callOnClick();
                })
                .show();
    }

    private void startActivityWithSettings(String settingsAction) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + getPackageName()));
        settingsPermissionActivityResultLauncher.launch(intent);
    }

}
