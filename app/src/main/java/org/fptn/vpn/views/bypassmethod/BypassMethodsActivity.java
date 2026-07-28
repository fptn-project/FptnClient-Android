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

package org.fptn.vpn.views.bypassmethod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elvishew.xlog.XLog;
import android.widget.AutoCompleteTextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.fptn.vpn.R;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionStrategy;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.snichecker.SniCheckerService;
import org.fptn.vpn.services.snichecker.SniCheckerServiceState;
import org.fptn.vpn.utils.ViewUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.adapter.ServerEntityAdapter;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.List;
import java.util.Optional;

public class BypassMethodsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private View sniLayout;

    private BypassMethodsViewModel viewModel;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private AlertDialog autoSelectDialog;

    private ServiceConnection connection;

    @Override
    protected void onStart() {
        super.onStart();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                XLog.tag(TAG).i("SNI checker service connected [component=%s]", name.getShortClassName());
                SniCheckerService.LocalBinder localBinder = (SniCheckerService.LocalBinder) service;
                viewModel.subscribeService(localBinder.getService());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                try {
                    if (viewModel != null) {
                        viewModel.unsubscribe();
                    }
                } catch (Exception e) {
                    XLog.tag(TAG).e("Error handling SNI checker service disconnect: %s", e.getMessage());
                }
            }
        };
        SniCheckerService.bindService(this, connection);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (connection != null) {
                unbindService(connection);
            }
        } catch (Exception e) {
            XLog.tag(TAG).e("Error unbinding SNI checker service: %s", e.getMessage());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_bypass_methods_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(BypassMethodsViewModel.class);

        sniLayout = findViewById(R.id.sni_layout);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        CustomBottomNavigationListener bottomNavigationListener = new CustomBottomNavigationListener(this, R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(bottomNavigationListener);
        bottomNavigationView.setOnItemReselectedListener(bottomNavigationListener);

        Spinner connectionStrategySpinner = findViewById(R.id.connection_strategy_spinner);
        ArrayAdapter<ConnectionStrategy> connectionStrategyAdapter = new ArrayAdapter<>(
                this,
                R.layout.sni_mode_spinner_item,
                R.id.sni_mode_label,
                ConnectionStrategy.values()
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getConnectionStrategyFriendlyName(getItem(position)));
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getConnectionStrategyFriendlyName(getItem(position)));
                return textView;
            }
        };
        connectionStrategySpinner.setAdapter(connectionStrategyAdapter);

        viewModel.getConnectionStrategyMutableLiveData().observe(this, strategy -> {
            int position = connectionStrategyAdapter.getPosition(strategy);
            if (position >= 0) {
                connectionStrategySpinner.setSelection(position);
            }
        });

        connectionStrategySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                ConnectionStrategy selected = (ConnectionStrategy) parent.getItemAtPosition(position);
                XLog.tag(TAG).i("Connection strategy selected [strategy=%s]", selected);
                viewModel.setConnectionStrategy(selected);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Setup RadioGroup listener
        RadioGroup protocolRadioGroup = findViewById(R.id.bypass_method_radio_button_group);
        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sni_reality_radio_button) {
                XLog.tag(TAG).i("Bypass method selected [method=SNI_REALITY]");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY);
            } else if (checkedId == R.id.obfuscation_radio_button) {
                XLog.tag(TAG).i("Bypass method selected [method=TLS_OBFUSCATION]");
                viewModel.setBypassMethod(BypassCensorshipMethod.TLS_OBFUSCATION);
            }
        });

        RadioButton sniSpoofingRadioButton = findViewById(R.id.sni_reality_radio_button);
        RadioButton obfuscationRadioButton = findViewById(R.id.obfuscation_radio_button);

        // Inside initializeVariable() method
        Spinner sniSpoofingModeSpinner = findViewById(R.id.sni_spoofing_mode_spinner);
        ArrayAdapter<SniSpoofingMode> adapter = new ArrayAdapter<>(
                this,
                R.layout.sni_mode_spinner_item,
                R.id.sni_mode_label,
                SniSpoofingMode.values()
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getSniSpoofingModeFriendlyName(getItem(position)));
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getSniSpoofingModeFriendlyName(getItem(position)));
                return textView;
            }
        };
        sniSpoofingModeSpinner.setAdapter(adapter);

        View sniAutoscanLayout = findViewById(R.id.sni_autoscan_layout);

        viewModel.getBypassCensorshipMethodMutableLiveData().observe(this, bypassCensorshipMethod -> {
            switch (bypassCensorshipMethod) {
                case TLS_OBFUSCATION:
                    obfuscationRadioButton.setChecked(true);
                    ViewUtils.hideView(sniLayout);
                    ViewUtils.hideView(sniSpoofingModeSpinner);
                    ViewUtils.hideView(sniAutoscanLayout);
                    break;
                case SNI_REALITY:
                    sniSpoofingRadioButton.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    ViewUtils.showView(sniSpoofingModeSpinner);
                    ViewUtils.showView(sniAutoscanLayout);
                    break;
            }
        });


        viewModel.getSniSpoofingModeMutableLiveData().observe(this, mode -> {
            int position = adapter.getPosition(mode);
            if (position >= 0) {
                sniSpoofingModeSpinner.setSelection(position);
            }
        });

        sniSpoofingModeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                SniSpoofingMode selectedMode = (SniSpoofingMode) parent.getItemAtPosition(position);
                viewModel.setSniSpoofingMode(selectedMode);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });


        // SNI field
        TextView sniTextField = findViewById(R.id.SNI_text_field);
        viewModel.getSniMutableLiveData().observe(this, sniTextField::setText);

        View editSniLayout = findViewById(R.id.edit_sni_layout);
        editSniLayout.setOnClickListener(this::onEditSNIServer);

        // SNI Auto
        TextView sniCountLabel = findViewById(R.id.loaded_sni_count_label);

        // Remove for temporary
        // Button loadSniButton = findViewById(R.id.load_sni_button);
        // loadSniButton.setOnClickListener(view -> onLoadButtonClicked());
        // Button deleteOrResetToDefaultSniButton = findViewById(R.id.delete_or_reset_sni_button);
        // deleteOrResetToDefaultSniButton.setOnClickListener(view -> onDeleteOrResetSniButtonClicked());
        try {
            viewModel.loadDefaultSni();  // Load default SNI
        } catch (PVNClientException err) {
            XLog.tag(TAG).w("Failed to load default SNI list: %s", err.errorMessage);
        }
        ToggleButton startStopCheckingSniButton = findViewById(R.id.auto_select_sni_button);
        startStopCheckingSniButton.setOnClickListener(v -> onAutoSelectSniClicked());

        ProgressBar sniProgressBar = findViewById(R.id.sni_checking_progress_bar);
        TextView sniProgressBarLabel = findViewById(R.id.sni_checking_progress_bar_label);
        viewModel.getCurrentProgress().observe(this, progressPair -> {
            if (progressPair != null) {
                Integer max = progressPair.second;
                sniProgressBar.setMax(max);
                Integer progress = progressPair.first;
                sniProgressBar.setProgress(progress);

                sniProgressBarLabel.setText(String.format("%d/%d", progress, max));
            }
        });

        TextView currentCheckingSni = findViewById(R.id.current_sni_text);
        viewModel.getCurrentCheckingSniInfo().observe(this, currentCheckingSni::setText);

        TextView checkingServerTextView = findViewById(R.id.selected_server_text);
        viewModel.getSelectedServer().observe(this, server -> checkingServerTextView.setText(server.getServerInfo()));

        View checkingInProgressView = findViewById(R.id.checking_in_progress_view);

        // todo: disable navigation bar when sni checking active
        //View settingsMenuItem = findViewById(R.id.menuSettings);
        viewModel.getServiceState().observe(this, serviceState -> {
            if (serviceState == SniCheckerServiceState.ACTIVE) {
                ViewUtils.showView(checkingInProgressView);

                startStopCheckingSniButton.setChecked(true);
            } else {
                ViewUtils.hideView(checkingInProgressView);

                startStopCheckingSniButton.setChecked(false);
                //settingsMenuItem.setEnabled(true);

                // todo: this is for what?
                viewModel.refreshCurrentSni();
            }
        });

        viewModel.getSniCountLiveData().observe(this,
                count -> {
                    sniCountLabel.setText(String.valueOf(count));

                    boolean isEnabled = count > 0;
                    //deleteOrResetToDefaultSniButton.setText(count > 0 ? R.string.delete_loaded_sni : R.string.load_default);
                    startStopCheckingSniButton.setEnabled(isEnabled);
                }
        );

        // Register the activity result launcher
        // This must be done in onCreate or as a class member initializer.
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            XLog.tag(TAG).i("SNI file selected [path=%s]", uri.getPath());
                            try {
                                viewModel.readFileContent(uri);
                            } catch (PVNClientException e) {
                                Toast.makeText(BypassMethodsActivity.this, e.errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        XLog.tag(TAG).i("SNI file selection cancelled");
                    }
                });

        viewModel.getFoundedSniEvent().observe(this, sni -> {
            if (sni != null) {
                Toast.makeText(this, "Found SNI: " + sni, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onAutoSelectSniClicked() {
        if (viewModel.getServiceState().getValue() == SniCheckerServiceState.INACTIVE) {
            Futures.addCallback(viewModel.getAllServers(), new FutureCallback<>() {
                @Override
                public void onSuccess(List<ServerEntity> servers) {
                    showAutoSelectDialog(servers);
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Failed to load server list for SNI auto-select: %s", t.getMessage());
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            SniCheckerService.stopChecking(this);
        }
    }

    private void showAutoSelectDialog(List<ServerEntity> serverEntities) {
        // Prevent creating multiple dialogs
        if (autoSelectDialog != null && autoSelectDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the custom layout
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_autoselect_sni, null);
        builder.setView(dialogView);

        // --- Setup Spinner ---
        ServerEntityAdapter serverEntityAdapter = new ServerEntityAdapter(serverEntities, R.layout.home_list_recycler_server_item);

        Spinner serverSpinner = dialogView.findViewById(R.id.dialog_server_spinner);
        serverSpinner.setAdapter(serverEntityAdapter);

        CheckBox resetCheckedCheckbox = dialogView.findViewById(R.id.reset_checked_checkbox);

        // --- Setup Buttons ---
        Button buttonCancel = dialogView.findViewById(R.id.dialog_button_cancel);
        Button buttonStart = dialogView.findViewById(R.id.dialog_button_start);

        // Create the dialog before setting click listeners to allow for dismissing it
        autoSelectDialog = builder.create();

        buttonCancel.setOnClickListener(v -> {
            XLog.tag(TAG).i("SNI auto-select cancelled by user");

            ToggleButton startStopCheckingSniButton = findViewById(R.id.auto_select_sni_button);
            startStopCheckingSniButton.setChecked(false);

            autoSelectDialog.dismiss();
        });

        buttonStart.setOnClickListener(v -> {
            // Get the originally selected server object
            int selectedPosition = serverSpinner.getSelectedItemPosition();
            ServerEntity selectedServer = serverEntities.get(selectedPosition);

            XLog.tag(TAG).i("Starting SNI auto-select [server=%s, resetChecked=%b]", selectedServer.getServerInfo(), resetCheckedCheckbox.isChecked());
            SniCheckerService.startChecking(this, selectedServer, resetCheckedCheckbox.isChecked(),
                    viewModel.getBypassCensorshipMethodMutableLiveData().getValue());

            autoSelectDialog.dismiss();
        });

        autoSelectDialog.show();
    }

    private void onDeleteOrResetSniButtonClicked() {
        // todo: if default sni files will more than two - maybe add selector in dialog, which to load
        int sniCount = viewModel.getSniCountLiveData().getValue();
        if (sniCount > 0) {
            viewModel.deleteAllSni();
        } else {
            try {
                viewModel.loadDefaultSni();
            } catch (PVNClientException e) {
                Toast.makeText(BypassMethodsActivity.this, e.errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onLoadButtonClicked() {
        // Create an intent to open the file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            // Launch the intent using the ActivityResultLauncher
            filePickerLauncher.launch(Intent.createChooser(intent, "Select a *.sni file"));
        } catch (ActivityNotFoundException ex) {
            // Potentially handle the case where the device has no file manager
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onEditSNIServer(View view) {
        View inflated = View.inflate(this, R.layout.sni_dialog_layout, null);

        AutoCompleteTextView sniEditText = inflated.findViewById(R.id.text_edit_sni);
        sniEditText.setText(viewModel.getCurrentSni());

        List<String> suggestions = loadSniSuggestions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, suggestions);
        sniEditText.setAdapter(adapter);

        com.google.android.material.textfield.TextInputLayout inputLayout =
                inflated.findViewById(R.id.sni_input_layout);
        inputLayout.setEndIconOnClickListener(v -> sniEditText.showDropDown());

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(inflated);
        alertDialogBuilder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            XLog.tag(TAG).i("SNI edit saved [sni=%s]", sniEditText.getText());
            Optional.ofNullable(sniEditText.getText())
                    .map(Object::toString)
                    .ifPresent(viewModel::validateAndSetSni);
        });
        alertDialogBuilder.setNeutralButton(getString(R.string.reset_default_button), (dialog, which) -> {
            XLog.tag(TAG).i("SNI reset to default");
            viewModel.resetToDefault();
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            XLog.tag(TAG).i("SNI edit cancelled");
        });
        alertDialogBuilder.show();
    }

    private List<String> loadSniSuggestions() {
        List<String> result = new ArrayList<>();
        if (Locale.getDefault().getLanguage().equals("ru")) {
            result.addAll(readSniRawFile(R.raw.russia));
        }
        result.addAll(readSniRawFile(R.raw.global));
        return result;
    }

    private List<String> readSniRawFile(int rawResId) {
        List<String> list = new ArrayList<>();
        try (InputStream is = getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines().forEach(line -> {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    list.add(trimmed);
                }
            });
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to read SNI suggestions file: %s", e.getMessage());
        }
        return list;
    }

    private String getConnectionStrategyFriendlyName(ConnectionStrategy strategy) {
        return switch (strategy) {
            case DUAL_TUNNEL -> getString(R.string.connection_strategy_dual);
            case ROLLING_TUNNEL -> getString(R.string.connection_strategy_rolling);
            case TRIPLE_TUNNEL -> getString(R.string.connection_strategy_triple);
        };
    }

    private String getSniSpoofingModeFriendlyName(SniSpoofingMode mode) {
        return switch (mode) {
            case SNI -> getString(R.string.sni);
            case SNI_REALITY_CHROME_149 ->
                    getString(R.string.sni_reality_radio_button_label_chrome_149);
            case SNI_REALITY_CHROME_148 ->
                    getString(R.string.sni_reality_radio_button_label_chrome_148);
            case SNI_REALITY_CHROME_147 ->
                    getString(R.string.sni_reality_radio_button_label_chrome_147);
            case SNI_REALITY_CHROME_146 ->
                    getString(R.string.sni_reality_radio_button_label_chrome_146);
            case SNI_REALITY_CHROME_145 ->
                    getString(R.string.sni_reality_radio_button_label_chrome_145);
            case SNI_REALITY_FIREFOX_151 ->
                    getString(R.string.sni_reality_radio_button_label_firefox_151);
            case SNI_REALITY_FIREFOX_150 ->
                    getString(R.string.sni_reality_radio_button_label_firefox_150);
            case SNI_REALITY_FIREFOX_149 ->
                    getString(R.string.sni_reality_radio_button_label_firefox_149);
            case SNI_REALITY_YANDEX_26_4 ->
                    getString(R.string.sni_reality_radio_button_label_yandex_26_4);
            case SNI_REALITY_YANDEX_26_3 ->
                    getString(R.string.sni_reality_radio_button_label_yandex_26_3);
            case SNI_REALITY_YANDEX_25 ->
                    getString(R.string.sni_reality_radio_button_label_yandex_25);
            case SNI_REALITY_YANDEX_24 ->
                    getString(R.string.sni_reality_radio_button_label_yandex_24);
            case SNI_REALITY_SAFARI_26_5 ->
                    getString(R.string.sni_reality_radio_button_label_safari_26_5);
            case SNI_REALITY_SAFARI_26_4 ->
                    getString(R.string.sni_reality_radio_button_label_safari_26_4);
            default -> mode.toString();
        };
    }
}
