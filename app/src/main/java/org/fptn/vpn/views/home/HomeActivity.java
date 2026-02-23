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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.vpn.FptnServiceState;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.CustomSpinner;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.adapter.ServerEntityAdapter;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

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
                    Log.e(TAG, "Error in onServiceDisconnected: " + e.getMessage());
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
            Log.e(TAG, "Error unbinding service: " + e.getMessage());
        }
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        ServerEntityAdapter serverEntityAdapter = new ServerEntityAdapter(R.layout.home_list_recycler_server_item);
        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter(serverEntityAdapter);

        startStopButton = findViewById(R.id.home_do_connect_button);
        startStopButton.setOnClickListener(this::onClickToStartStop);

        /*View containers to hide*/
        homeSpeedFrame = findViewById(R.id.home_speed_frame);
        connectionTimeFrame = findViewById(R.id.home_connection_timer_frame);
        serverInfoFrame = findViewById(R.id.home_server_info_frame);

        viewModel = new ViewModelProvider(this).get(HomeActivityViewModel.class);
        viewModel.getServerDtoListLiveData().observe(this, serverEntities -> {
            ((ServerEntityAdapter) spinnerServers.getAdapter()).setServerEntityList(serverEntities);

            for (int i = 0; i < serverEntities.size(); i++) {
                if (serverEntities.get(i).isSelected()) {
                    spinnerServers.setSelection(i);
                }
            }

            spinnerServers.performClosedEvent(); // FIX SPINNER BACKGROUND
        });

        View settingsMenuItem = findViewById(R.id.menuSettings);
        viewModel.getServiceStateMutableLiveData().observe(this, fptnServiceState -> {
            // we can't change UI from viewModel
            switch (fptnServiceState.getConnectionState()) {
                case CONNECTED:
                    connectedStateUiItems();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
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
                    String errorText = Optional.ofNullable(viewModel.getErrorTextLiveData().getValue())
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

        connectionTimerTextView = findViewById(R.id.home_connection_timer);
        viewModel.getTimerTextLiveData().observe(this, timerText -> connectionTimerTextView.setText(timerText));

        errorTextView = findViewById(R.id.home_error_text_view);
        viewModel.getErrorTextLiveData().observe(this, errorCodeText -> errorTextView.setText(errorCodeText));

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
        if (!PermissionsUtils.isAllOptionalPermissionsGranted(this)) {
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
                    Log.i(TAG, "Permission granted!");
                } else {
                    Log.i(TAG, "Permission disabled!");
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
                    Log.i(TAG, "Permissions request denied!");
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
