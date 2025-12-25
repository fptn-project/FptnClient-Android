package org.fptn.vpn.views;

import android.Manifest;
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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.CustomVpnServiceState;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.CustomSpinner;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.adapter.FptnServerAdapter;
import org.fptn.vpn.services.CustomVpnService;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Getter
    private FptnServerViewModel fptnViewModel;

    private TextView connectionTimerTextView;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView statusTextView;
    private TextView errorTextView;

    private TextView connectedServerTextView;

    private View connectionTimeFrame;
    private View serverInfoFrame;
    private View homeSpeedFrame;
    private View permissionWarningFrame;

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
                Log.i(TAG, "onServiceConnected: " + name);
                CustomVpnService.LocalBinder localBinder = (CustomVpnService.LocalBinder) service;
                fptnViewModel.subscribeService(localBinder.getService());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                try {
                    if (fptnViewModel != null) {
                        fptnViewModel.unsubscribe();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onServiceDisconnected: " + e.getMessage());
                }
            }
        };
        CustomVpnService.bindService(this, connection);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (connection != null) {
                unbindService(connection);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unbinding service: " + e.getMessage());
        }
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        spinnerServers = findViewById(R.id.home_server_spinner);

        startStopButton = findViewById(R.id.home_do_connect_button);
        startStopButton.setOnClickListener(this::onClickToStartStop);

        downloadTextView = findViewById(R.id.home_download_speed);
        uploadTextView = findViewById(R.id.home_upload_speed);
        connectionTimerTextView = findViewById(R.id.home_connection_timer);
        connectedServerTextView = findViewById(R.id.home_connected_server_name);
        statusTextView = findViewById(R.id.home_connection_status);
        errorTextView = findViewById(R.id.home_error_text_view);

        /*View containers to hide*/
        homeSpeedFrame = findViewById(R.id.home_speed_frame);
        connectionTimeFrame = findViewById(R.id.home_connection_timer_frame);
        serverInfoFrame = findViewById(R.id.home_server_info_frame);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getServerDtoListLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                List<FptnServerDto> fixedServers = new ArrayList<>();
                fixedServers.add(FptnServerDto.AUTO);
                fixedServers.addAll(fptnServerDtos);
                FptnServerAdapter fptnServerAdapter = new FptnServerAdapter(fixedServers,
                        R.layout.home_list_recycler_server_item);
                spinnerServers.setAdapter(fptnServerAdapter);

                int i = 0;
                for (FptnServerDto fixedServer : fixedServers) {
                    if (fixedServer.isSelected) {
                        spinnerServers.setSelection(i);
                        connectedServerTextView.setText(fixedServer.getServerInfo());
                    }
                    i++;
                }

                spinnerServers.performClosedEvent(); // FIX SPINNER BACKGROUND
            } else {
                // goto Login activity
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        View settingsMenuItem = findViewById(R.id.menuSettings);
        fptnViewModel.getServiceStateMutableLiveData().observe(this, customVpnServiceState -> {
            // we can't change UI from viewModel
            switch (customVpnServiceState.getConnectionState()) {
                case CONNECTED:
                    connectedStateUiItems();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
                    break;
                default:
                    break;
            }

            boolean activeState = customVpnServiceState.getConnectionState().isActiveState();
            startStopButton.setChecked(activeState);
            spinnerServers.setEnabled(!activeState);
            settingsMenuItem.setEnabled(!activeState);

            // we can't show Snackbar from viewModel
            PVNClientException exception = customVpnServiceState.getException();
            if (exception != null) {
                if (ErrorCode.Companion.isNeedToOfferRefreshToken(exception.errorCode)) {
                    String errorText = Optional.ofNullable(fptnViewModel.getErrorTextLiveData().getValue())
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

        fptnViewModel.getDownloadSpeedAsStringLiveData().observe(this, downloadSpeed -> downloadTextView.setText(downloadSpeed));
        fptnViewModel.getUploadSpeedAsStringLiveData().observe(this, uploadSpeed -> uploadTextView.setText(uploadSpeed));
        fptnViewModel.getTimerTextLiveData().observe(this, timerText -> connectionTimerTextView.setText(timerText));

        fptnViewModel.getErrorTextLiveData().observe(this, errorCodeText -> errorTextView.setText(errorCodeText));
        fptnViewModel.getStatusTextLiveData().observe(this, statusText -> statusTextView.setText(statusText));

        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuHome);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuHome));

        permissionWarningFrame = findViewById(R.id.home_permission_warning_frame);

        // hide
        disconnectedStateUiItems();

        requestAddTileService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing() && !isDestroyed()) {
            bottomNavigationView.setSelectedItemId(R.id.menuHome);
        }
    }

    private void disconnectedStateUiItems() {
        hideView(connectionTimeFrame);
        hideView(serverInfoFrame);
        hideView(homeSpeedFrame);
        hideView(permissionWarningFrame);

        showView(spinnerServers);
    }

    private void connectedStateUiItems() {
        showView(connectionTimeFrame);
        showView(serverInfoFrame);
        showView(homeSpeedFrame);

        // check is need to show permissions warning
        if (!PermissionsUtils.isAllPermissionsGranted(this)) {
            showView(permissionWarningFrame);
        }

        hideView(spinnerServers);
    }

    private void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void onClickToStartStop(View v) {
        ConnectionState currentConnectionState = Optional.ofNullable(fptnViewModel.getServiceStateMutableLiveData().getValue())
                .map(CustomVpnServiceState::getConnectionState)
                .orElse(ConnectionState.DISCONNECTED);
        if (currentConnectionState == ConnectionState.DISCONNECTED) {
            // Check if all permissions are granted
            if (!PermissionsUtils.isAllPermissionsGranted(this)) {
                // Show message that all permissions are required
                Toast.makeText(this, R.string.all_permissions_required, Toast.LENGTH_LONG).show();

                // Reset button state
                startStopButton.setChecked(false);

                // Request all required permissions
                requestRequiredPermissions();
                return;
            }

            // All permissions are granted, now check VPN permission
            Intent intent = VpnService.prepare(this);
            if (intent != null) {
                // Request to user on launch vpn
                vpnPermissionActivityResultLauncher.launch(intent);
                // we don't know result of vpn permission request yet
                startStopButton.setChecked(false);
            } else {
                // All permissions are granted, start VPN connection
                // explicit assignment cause service may start slowly
                fptnViewModel.getServiceStateMutableLiveData().postValue(CustomVpnServiceState.FAKE_CONNECTING);

                CustomVpnService.startToConnect(this, (FptnServerDto) spinnerServers.getSelectedItem());
            }
        } else {
            if (currentConnectionState.isActiveState()) {
                CustomVpnService.startToDisconnect(this);
            }
        }
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
                                    Log.d(TAG, "Tile already added successfully. Nothing to do.");
                                    Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT)
                                            .show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Log.d(TAG, "Tile added successfully.");
                                    Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Log.d(TAG, "User cancel request.");
                                }
                            }
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Failed to request tile addition", e);
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
                    // Check again if all permissions are granted before starting VPN
                    if (!PermissionsUtils.isAllPermissionsGranted(this)) {
                        Toast.makeText(this, R.string.all_permissions_required, Toast.LENGTH_LONG).show();
                        startStopButton.setChecked(false);
                        return;
                    }

                    CustomVpnService.startToConnect(this, (FptnServerDto) spinnerServers.getSelectedItem());
                } else {
                    Toast.makeText(this, R.string.vpn_permission_warning, Toast.LENGTH_SHORT).show();
                    fptnViewModel.getErrorTextLiveData().postValue(getString(R.string.vpn_permission_warning));
                    startStopButton.setChecked(false);
                }
            }
    );

    private final AtomicInteger requestedPermissions = new AtomicInteger(0);

    private final ActivityResultLauncher<String> showNotificationActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "Notifications enabled!");
                } else {
                    Log.i(TAG, "Notifications disabled!");
                }

                // Check if all permissions are granted after this permission request
                checkAndCompletePermissionRequest();
            }
    );

    private final ActivityResultLauncher<Intent> settingsPermissionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            activityResult -> {
                Log.i(TAG, "Returned from settings activity");

                // Check if all permissions are granted after returning from settings
                checkAndCompletePermissionRequest();
            }
    );

    // Helper method to check permissions and complete the request
    private void checkAndCompletePermissionRequest() {
        if (requestedPermissions.decrementAndGet() == 0) {
            // All permission requests have completed
            if (PermissionsUtils.isAllPermissionsGranted(this)) {
                // All permissions are granted, proceed with VPN connection
                startStopButton.callOnClick();
            } else {
                // Still missing some permissions
                Toast.makeText(this, R.string.all_permissions_required, Toast.LENGTH_LONG).show();
                startStopButton.setChecked(false);
            }
        }
    }

    /* PERMISSIONS PART */
    @SuppressLint("BatteryLife")
    private void requestRequiredPermissions() {
        requestedPermissions.set(0); // Reset counter

        // Check notification permission first - handle special case for "Don't ask again"
        final boolean[] shouldRequestNotificationPermission = {false};
        final boolean[] shouldOpenSettingsForNotifications = {false};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsUtils.checkNotificationPermission(this)) {
                // Check if we can request the permission or need to open settings
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    // We can request the permission
                    shouldRequestNotificationPermission[0] = true;
                    requestedPermissions.incrementAndGet();
                } else {
                    // Permission was denied with "Don't ask again", need to open settings
                    shouldOpenSettingsForNotifications[0] = true;
                    requestedPermissions.incrementAndGet();
                }
            }
        }

        // Check battery optimizations
        final boolean[] shouldRequestBatteryOptimization = {false};
        if (!PermissionsUtils.checkBatteryOptimizations(this)) {
            shouldRequestBatteryOptimization[0] = true;
            requestedPermissions.incrementAndGet();
        }

        // Check background data restrictions
        final boolean[] shouldRequestBackgroundData = {false};
        if (!PermissionsUtils.checkBackgroundDataTransferRestrictions(this)) {
            shouldRequestBackgroundData[0] = true;
            requestedPermissions.incrementAndGet();
        }

        if (requestedPermissions.get() == 0) {
            // All permissions are already granted
            startStopButton.callOnClick();
            return;
        }

        // Show dialog explaining why permissions are needed
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_request_title))
                .setMessage(getString(R.string.permission_request_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    // Request notification permission if needed
                    if (shouldRequestNotificationPermission[0]) {
                        showNotificationActivityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else if (shouldOpenSettingsForNotifications[0]) {
                        // Open app settings for notification permission
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        settingsPermissionActivityResultLauncher.launch(intent);
                    }

                    // Request battery optimization permission
                    if (shouldRequestBatteryOptimization[0]) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        settingsPermissionActivityResultLauncher.launch(intent);
                    }

                    // Request background data restriction permission
                    if (shouldRequestBackgroundData[0]) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        settingsPermissionActivityResultLauncher.launch(intent);
                    }
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Permissions request denied!");
                    Toast.makeText(this, R.string.all_permissions_required, Toast.LENGTH_LONG).show();
                    startStopButton.setChecked(false);
                })
                .setOnCancelListener(dialog -> {
                    Log.i(TAG, "Permissions request cancelled!");
                    Toast.makeText(this, R.string.all_permissions_required, Toast.LENGTH_LONG).show();
                    startStopButton.setChecked(false);
                })
                .show();
    }
}
