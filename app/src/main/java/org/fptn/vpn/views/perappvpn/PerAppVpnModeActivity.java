package org.fptn.vpn.views.perappvpn;

import static org.fptn.vpn.utils.ViewUtils.hideView;
import static org.fptn.vpn.utils.ViewUtils.showView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.views.CustomBottomNavigationListener;

public class PerAppVpnModeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private PerAppVpnModeViewModel viewModel;

    private View selectAppsListLayout;
    private RecyclerView appListRecyclerView;
    private EditText searchAppsEditText;

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

        searchAppsEditText = findViewById(R.id.search_apps_edit_text);
        searchAppsEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter adapter) {
                    adapter.filter(s.toString());
                }
            }
        });

        SwitchCompat showSystemAppsSwitch = findViewById(R.id.show_system_apps_switch);
        showSystemAppsSwitch.setChecked(viewModel.isShowSystemApps());
        showSystemAppsSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                viewModel.setShowSystemApps(isChecked));

        // Setup RadioGroup listener
        RadioGroup protocolRadioGroup = findViewById(R.id.per_app_vpn_mode_radio_button_group);
        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.all_app_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=OFF]");
                viewModel.setPerAppVpnMode(PerAppVpnMode.OFF);
            } else if (checkedId == R.id.allowed_apps_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=ONLY_ALLOWED]");
                viewModel.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED);
            } else if (checkedId == R.id.disallowed_apps_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=EXCEPT_DISALLOWED]");
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
            String query = searchAppsEditText.getText().toString();
            if (!query.isEmpty()) adapter.filter(query);
            appListRecyclerView.setAdapter(adapter);

            hideView(progressBar);
        });

        // Trigger the load
        viewModel.loadInstalledApps(getPackageManager());

        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            XLog.tag(TAG).i("Per-app VPN mode changes cancelled");
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            PerAppVpnMode mode = viewModel.getPerAppVpnModeMutableLiveData().getValue();
            if (mode == PerAppVpnMode.ONLY_ALLOWED && !viewModel.hasSelectedApps()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.per_app_vpn_no_apps_selected_title)
                        .setMessage(R.string.per_app_vpn_no_apps_selected_message)
                        .setPositiveButton(R.string.save_anyway, (dialog, which) -> {
                            XLog.tag(TAG).i("Per-app VPN mode saved with no apps selected [mode=%s]", mode);
                            viewModel.saveAllSettings();
                            finish();
                        })
                        .setNegativeButton(R.string.cancel_button, null)
                        .show();
                return;
            }
            XLog.tag(TAG).i("Per-app VPN mode saved [mode=%s]", mode);
            viewModel.saveAllSettings();
            finish();
        });
    }
}