package org.fptn.vpn.views.perappvpn;

import static org.fptn.vpn.utils.ViewUtils.hideView;
import static org.fptn.vpn.utils.ViewUtils.showView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.views.CustomBottomNavigationListener;

public class PerAppVpnModeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private PerAppVpnModeViewModel viewModel;

    private View selectAppsListLayout;
    private RecyclerView appListRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_per_app_vpnmode_layout);

        initializeVariable();
    }

    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(PerAppVpnModeViewModel.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));

        selectAppsListLayout = findViewById(R.id.select_apps_list_layout);

        // Setup RadioGroup listener
        RadioGroup protocolRadioGroup = findViewById(R.id.per_app_vpn_mode_radio_button_group);
        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.all_app_mode_radio_button) {
                Log.d(TAG, "Selected per-app-VPN mode off");
                viewModel.setPerAppVpnMode(PerAppVpnMode.OFF);
            } else if (checkedId == R.id.allowed_apps_mode_radio_button) {
                Log.d(TAG, "Selected per-app-VPN mode only allowed");
                viewModel.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED);
            } else if (checkedId == R.id.disallowed_apps_mode_radio_button) {
                Log.d(TAG, "Selected per-app-VPN mode except disallowed");
                viewModel.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED);
            }
        });

        RadioButton perAppVpnModeOffRadioButton = findViewById(R.id.all_app_mode_radio_button);
        RadioButton onlyAllowedAppsRadioButton = findViewById(R.id.allowed_apps_mode_radio_button);
        RadioButton exceptDisAllowedRadioButton = findViewById(R.id.disallowed_apps_mode_radio_button);
        viewModel.getPerAppVpnModeMutableLiveData().observe(this, perAppVpnMode -> {
            switch (perAppVpnMode) {
                case OFF:
                    perAppVpnModeOffRadioButton.setChecked(true);
                    hideView(selectAppsListLayout);
                    break;
                case ONLY_ALLOWED:
                    onlyAllowedAppsRadioButton.setChecked(true);
                    showView(selectAppsListLayout);

                    if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter appInfoListAdapter) {
                        appInfoListAdapter.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED);
                    }

                    break;
                case EXCEPT_DISALLOWED:
                    exceptDisAllowedRadioButton.setChecked(true);
                    showView(selectAppsListLayout);

                    if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter appInfoListAdapter) {
                        appInfoListAdapter.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED);
                    }

                    break;
            }
        });

        appListRecyclerView = findViewById(R.id.apps_recycler_view);
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Show a progress bar while loading if you have one in your XML
        ProgressBar progressBar = findViewById(R.id.loading_apps_progress_bar);

        viewModel.getAppListMutableLiveData().observe(this, apps -> {
            AppInfoListAdapter adapter = new AppInfoListAdapter(apps, viewModel.getPerAppVpnModeMutableLiveData().getValue());
            appListRecyclerView.setAdapter(adapter);

            hideView(progressBar);
        });

        // Trigger the load
        viewModel.loadInstalledApps(getPackageManager());

        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");

            viewModel.saveAllSettings();

            finish();
        });
    }
}