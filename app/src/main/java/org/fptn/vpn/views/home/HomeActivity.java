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
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import org.fptn.vpn.views.updatetoken.UpdateTokenActivity;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Optional;

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
    private View trafficChartDivider;
    private int trafficFrameMinHeight;

    private CustomSpinner spinnerServers;

    private ToggleButton startStopButton;

    // Background-setup checklist dialog (notifications / battery / pin), refreshed on resume
    private AlertDialog backgroundSetupDialog;
    private ImageView notificationsStateIcon;
    private ImageView batteryStateIcon;
    private ImageView pinStateIcon;
    private Button backgroundSetupContinueButton;
    // Notifications/battery are gated on the real grant state; only the Xiaomi "lock in Security"
    // step (which can't be read back) is gated on the user having opened it.
    private boolean visitedPin;
    private boolean connectAfterBackgroundSetup;
    // POST_NOTIFICATIONS permanently denied — the system dialog won't show again, use settings.
    private boolean notificationPermanentlyDenied;

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
        trafficChartDivider = findViewById(R.id.home_traffic_chart_divider);
        trafficFrameMinHeight = ((ConstraintLayout.LayoutParams) homeTrafficFrame.getLayoutParams()).matchConstraintMinHeight;
        applyTrafficChartVisibility();
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
                if (exception.errorCode == ErrorCode.VPN_INTERFACE_ERROR) {
                    showVpnSetupErrorDialog();
                } else if (ErrorCode.Companion.isNeedToOfferRefreshToken(exception.errorCode)) {
                    String errorText = Optional.ofNullable(viewModel.getStatusTextLiveData().getValue())
                            .orElse(ErrorCode.UNKNOWN_ERROR.getValue());
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.layout), errorText, 8000);
                    snackbar.setAction(getString(R.string.refresh_token), v -> {
                        Intent browserIntent = new
                                Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.telegram_bot_link)));
                        startActivity(browserIntent);
                    });
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
        // Re-entry point: tapping the warning re-opens the checklist (without forcing a connect).
        permissionWarningFrame.setOnClickListener(v -> showBackgroundSetupDialog(false));
        final int warningBasePadding = permissionWarningFrame.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(permissionWarningFrame, (v, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), warningBasePadding + statusBarTop,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // hide
        disconnectedStateUiItems();

        requestAddTileService();
    }

    private static final long TOKEN_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000;

    private boolean maybeShowTokenReminder() {
        long age = System.currentTimeMillis() - SharedPrefUtils.getTokenUpdatedDate(this);
        if (age < TOKEN_MAX_AGE_MS) {
            return false;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.token_reminder_title)
                .setMessage(Html.fromHtml(getString(R.string.token_reminder_message), Html.FROM_HTML_MODE_LEGACY))
                .setCancelable(false)
                .setPositiveButton(R.string.token_reminder_update, (d, w) -> {
                    startStopButton.setChecked(false);
                    startActivity(new Intent(this, UpdateTokenActivity.class));
                })
                .setNegativeButton(R.string.token_reminder_later, (d, w) -> connectVpn())
                .create();
        dialog.show();
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return true;
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

        // Returned from a system settings screen — refresh the checklist marks.
        if (backgroundSetupDialog != null && backgroundSetupDialog.isShowing()) {
            refreshBackgroundSetupStates();
        }

        updatePermissionWarning();

        // The setting may have changed while this activity was paused (advanced settings screen).
        applyTrafficChartVisibility();

        Optional.ofNullable(viewModel.getServiceStateMutableLiveData())
                .map(LiveData::getValue)
                .map(FptnServiceState::getConnectionState)
                .ifPresent((state) -> {
                    if (state == ConnectionState.DISCONNECTED) {
                        viewModel.startCheckingPing();
                    }
                });
    }

    // Toggles only the speed chart (advanced setting); speed and traffic rows stay visible,
    // and the traffic card shrinks to its remaining content instead of keeping the chart's space.
    private void applyTrafficChartVisibility() {
        boolean showChart = SharedPrefUtils.getShowTrafficChart(this);
        trafficSpeedChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
        trafficChartDivider.setVisibility(showChart ? View.VISIBLE : View.GONE);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) homeTrafficFrame.getLayoutParams();
        params.height = showChart ? ConstraintLayout.LayoutParams.MATCH_CONSTRAINT : ConstraintLayout.LayoutParams.WRAP_CONTENT;
        params.verticalBias = showChart ? 0.5f : 0f;
        // The XML min height (140dp) keeps padding the card even in wrap mode — drop it with the chart.
        params.matchConstraintMinHeight = showChart ? trafficFrameMinHeight : 0;
        homeTrafficFrame.setLayoutParams(params);
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

        updatePermissionWarning();

        ViewUtils.hideView(spinnerServers);
    }

    private void updatePermissionWarning() {
        ConnectionState state = Optional.ofNullable(viewModel.getServiceStateMutableLiveData().getValue())
                .map(FptnServiceState::getConnectionState)
                .orElse(ConnectionState.DISCONNECTED);
        if (state == ConnectionState.CONNECTED && needsBackgroundSetup()) {
            ViewUtils.showView(permissionWarningFrame);
        } else {
            ViewUtils.hideView(permissionWarningFrame);
        }
    }

    // Single source of truth for "background setup incomplete": notifications + battery on every
    // device. The Xiaomi "lock in Security" step is optional guidance only — it never gates here,
    // since MIUI exposes no way to read it back and we don't want to nag users who already pinned.
    // Used by both the connect gate and the home warning banner.
    private boolean needsBackgroundSetup() {
        return !PermissionsUtils.checkNotificationEnabled(this)
                || !PermissionsUtils.checkBatteryOptimizations(this);
    }

    public void onClickToStartStop(View v) {
        ConnectionState currentConnectionState = Optional.ofNullable(viewModel.getServiceStateMutableLiveData().getValue())
                .map(FptnServiceState::getConnectionState)
                .orElse(ConnectionState.DISCONNECTED);
        if (currentConnectionState == ConnectionState.DISCONNECTED) {

            if (needsBackgroundSetup()) {
                startStopButton.setChecked(false);
                showBackgroundSetupDialog(true);
                // proceedToVpnConnect() is called from the dialog's Continue button — not here
                return;
            }

            proceedToVpnConnect();
        } else {
            if (currentConnectionState.isActiveState()) {
                FptnService.startToDisconnect(this);
            }
        }
    }

    private void proceedToVpnConnect() {
        if (PermissionsUtils.isAlwaysOnVpnEnabledByAnotherApp(this)) {
            startStopButton.setChecked(false);
            showVpnSwitchDialog();
            return;
        }
        if (maybeShowTokenReminder()) {
            return;
        }
        connectVpn();
    }

    private void showBackgroundSetupDialog(boolean connectOnDone) {
        connectAfterBackgroundSetup = connectOnDone;
        visitedPin = false;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_background_setup, null);
        // Buttons live in the dialog's fixed footer, so on small screens they stay visible while the
        // checklist itself scrolls. "Continue" (positive) is gated in refresh(); "Later" (negative)
        // is always available so the user is never trapped. Both dismiss and, in the connect flow,
        // proceed to connect. The checklist reappears next connect while something is outstanding.
        backgroundSetupDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(R.string.background_setup_done, (d, w) -> {
                    if (connectAfterBackgroundSetup) {
                        proceedToVpnConnect();
                    }
                })
                .setNegativeButton(R.string.background_setup_later, (d, w) -> {
                    if (connectAfterBackgroundSetup) {
                        proceedToVpnConnect();
                    }
                })
                .create();

        View rowNotifications = dialogView.findViewById(R.id.row_notifications);
        View rowBattery = dialogView.findViewById(R.id.row_battery);
        View rowPin = dialogView.findViewById(R.id.row_pin);
        notificationsStateIcon = dialogView.findViewById(R.id.row_notifications_state);
        batteryStateIcon = dialogView.findViewById(R.id.row_battery_state);
        pinStateIcon = dialogView.findViewById(R.id.row_pin_state);

        // Notifications and battery apply to every device; the "lock in Security" step is Xiaomi-only.
        rowPin.setVisibility(PermissionsUtils.isXiaomi() ? View.VISIBLE : View.GONE);

        // Rows open system screens; keep the dialog up so the user does every step and then taps
        // Continue. State refreshes on resume, so the check marks reflect what was actually granted.
        rowNotifications.setOnClickListener(v -> {
            startStopButton.setChecked(false);
            requestNotifications();
        });
        rowBattery.setOnClickListener(v -> {
            startStopButton.setChecked(false);
            SharedPrefUtils.saveBatteryOptimizationRequested(this, true);
            openBatteryOptimizationSettings();
        });
        rowPin.setOnClickListener(v -> {
            startStopButton.setChecked(false);
            visitedPin = true;
            SharedPrefUtils.saveXiaomiPinDone(this, true);
            refreshBackgroundSetupStates();
            PermissionsUtils.openMiuiSecurityApp(this);
        });

        // getButton() is only valid after show() — grab "Continue" then and gate it.
        backgroundSetupDialog.setOnShowListener(d -> {
            backgroundSetupContinueButton = backgroundSetupDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            refreshBackgroundSetupStates();
        });
        backgroundSetupDialog.setOnDismissListener(d -> {
            backgroundSetupDialog = null;
            notificationsStateIcon = null;
            batteryStateIcon = null;
            pinStateIcon = null;
            backgroundSetupContinueButton = null;
        });
        backgroundSetupDialog.show();
    }

    // Notifications and battery are gated on the REAL grant state (honest check marks). The Xiaomi
    // pin step is optional: its check mark reflects whether the user has opened it (persisted, since
    // MIUI exposes no way to read it back), but it never blocks the Continue button.
    // Called again on resume so state updates after returning from a system screen.
    private void refreshBackgroundSetupStates() {
        boolean notificationsDone = PermissionsUtils.checkNotificationEnabled(this);
        boolean batteryDone = PermissionsUtils.checkBatteryOptimizations(this);
        boolean pinDone = !PermissionsUtils.isXiaomi() || visitedPin || SharedPrefUtils.isXiaomiPinDone(this);

        if (notificationsStateIcon != null) {
            notificationsStateIcon.setImageResource(notificationsDone
                    ? R.drawable.ic_check_16 : R.drawable.ic_outline_arrow_forward_ios_16);
        }
        if (batteryStateIcon != null) {
            batteryStateIcon.setImageResource(batteryDone
                    ? R.drawable.ic_check_16 : R.drawable.ic_outline_arrow_forward_ios_16);
        }
        if (pinStateIcon != null) {
            pinStateIcon.setImageResource(pinDone
                    ? R.drawable.ic_check_16 : R.drawable.ic_outline_arrow_forward_ios_16);
        }
        if (backgroundSetupContinueButton != null) {
            backgroundSetupContinueButton.setEnabled(notificationsDone && batteryDone);
        }
    }

    private void requestNotifications() {
        if (PermissionsUtils.checkNotificationEnabled(this)) {
            refreshBackgroundSetupStates();
            return;
        }
        // Android 13+: one-tap runtime prompt while it's still available; otherwise settings.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermanentlyDenied) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            openNotificationSettings();
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            XLog.tag(TAG).w("Failed to open notification settings: %s", e.getMessage());
            PermissionsUtils.openMiuiBackgroundSettings(this);
        }
    }

    @SuppressLint("BatteryLife")
    private void openBatteryOptimizationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            PermissionsUtils.openMiuiBackgroundSettings(this);
        }
    }

    private void connectVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            vpnPermissionActivityResultLauncher.launch(intent);
            startStopButton.setChecked(false);
        } else {
            viewModel.getServiceStateMutableLiveData().postValue(FptnServiceState.FAKE_CONNECTING);
            FptnService.startToConnect(this, (ServerEntity) spinnerServers.getSelectedItem());
        }
    }

    private void showVpnSwitchDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.vpn_switch_title)
                .setMessage(R.string.vpn_switch_message)
                .setPositiveButton(R.string.vpn_switch_button, (d, w) -> connectVpn())
                .setNegativeButton(R.string.cancel_button, null)
                .show();
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
                    showVpnSetupErrorDialog();
                }
            }
    );

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                // If it wasn't granted and the system won't show the dialog again, the only path
                // left is the notification settings screen.
                if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    notificationPermanentlyDenied = true;
                }
                refreshBackgroundSetupStates();
            }
    );

}
