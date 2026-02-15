package org.fptn.vpn.views.bypassmethod;

import static org.fptn.vpn.utils.ViewUtils.hideView;
import static org.fptn.vpn.utils.ViewUtils.showView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.fptn.vpn.R;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.services.snichecker.SniCheckerService;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.adapter.ServerEntityAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BypassMethodsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private View sniLayout;
    private BypassMethodsViewModel viewModel;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private AlertDialog autoSelectDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_bypass_methods_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(BypassMethodsViewModel.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));

        // Setup RadioGroup listener
        RadioGroup protocolRadioGroup = findViewById(R.id.bypass_method_radio_button_group);
        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sni_spoofing_radio_button) {
                Log.d(TAG, "Selected SNI spoofing");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_SPOOFING);
            } else if (checkedId == R.id.obfuscation_radio_button) {
                Log.d(TAG, "Selected TLS obfuscation");
                viewModel.setBypassMethod(BypassCensorshipMethod.TLS_OBFUSCATION);
            } else if (checkedId == R.id.sni_reality_radio_button) {
                Log.d(TAG, "Selected SNI Reality");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY);
            }
        });

        RadioButton sniSpoofingRadioButton = findViewById(R.id.sni_spoofing_radio_button);
        RadioButton obfuscationRadioButton = findViewById(R.id.obfuscation_radio_button);
        RadioButton sniRealityRadioButton = findViewById(R.id.sni_reality_radio_button);
        viewModel.getBypassCensorshipMethodMutableLiveData().observe(this, bypassCensorshipMethod -> {
            switch (bypassCensorshipMethod) {
                case SNI_SPOOFING:
                    sniSpoofingRadioButton.setChecked(true);
                    showView(sniLayout);
                    break;
                case TLS_OBFUSCATION:
                    obfuscationRadioButton.setChecked(true);
                    hideView(sniLayout);
                    break;
                case SNI_REALITY:
                    sniRealityRadioButton.setChecked(true);
                    showView(sniLayout);
                    break;
            }
        });

        sniLayout = findViewById(R.id.sni_layout);
        sniLayout.setOnClickListener(this::onEditSNIServer);

        // SNI field
        TextView sniTextField = findViewById(R.id.SNI_text_field);
        viewModel.getSniMutableLiveData().observe(this, sniTextField::setText);

        View editSniButton = findViewById(R.id.imageView);
        editSniButton.setOnClickListener(this::onEditSNIServer);

        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");

            viewModel.saveBypassMethod();

            finish();
        });

        // SNI Auto
        TextView sniCountLabel = findViewById(R.id.loaded_sni_count_label);
        viewModel.getSniCountLiveData().observe(this,
                count -> sniCountLabel.setText(String.valueOf(count)));

        Button loadSniButton = findViewById(R.id.load_sni_button);
        loadSniButton.setOnClickListener(view -> onLoadButtonClicked());

        Button deleteSniButton = findViewById(R.id.delete_sni_button);
        deleteSniButton.setOnClickListener(view -> onDeleteButtonClicked());

        Button autoSelectSniButton = findViewById(R.id.auto_select_sni_button);
        autoSelectSniButton.setOnClickListener(v -> onAutoSelectSniClicked());

        // Register the activity result launcher
        // This must be done in onCreate or as a class member initializer.
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            Log.d(TAG, "File selected: " + uri.getPath());
                            readFileContent(uri);
                        }
                    } else {
                        Log.w(TAG, "File selection cancelled.");
                    }
                });
    }

    private void onAutoSelectSniClicked() {
        //if (viewModel.getSniCountLiveData().getValue() > 0)
        Futures.addCallback(viewModel.getAllServers(), new FutureCallback<>() {
            @Override
            public void onSuccess(List<ServerEntity> servers) {
                showAutoSelectDialog(servers);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, getMainExecutor());
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

        // --- Setup Buttons ---
        Button buttonCancel = dialogView.findViewById(R.id.dialog_button_cancel);
        Button buttonStart = dialogView.findViewById(R.id.dialog_button_start);

        // Create the dialog before setting click listeners to allow for dismissing it
        autoSelectDialog = builder.create();

        buttonCancel.setOnClickListener(v -> {
            Log.d(TAG, "Auto-select dialog cancelled.");
            autoSelectDialog.dismiss();
        });

        buttonStart.setOnClickListener(v -> {
            // Get the originally selected server object
            int selectedPosition = serverSpinner.getSelectedItemPosition();
            ServerEntity selectedServer = serverEntities.get(selectedPosition);

            Log.d(TAG, "Starting SNI auto-select for server: " + selectedServer.getServerInfo());
            Toast.makeText(this, "Starting auto-select for " + selectedServer.getServerInfo(), Toast.LENGTH_SHORT).show();

            // todo: add start AutoSearchSni service
            SniCheckerService.startChecking(this, selectedServer);

            autoSelectDialog.dismiss();

            finish();
        });

        autoSelectDialog.show();
    }

    private void onDeleteButtonClicked() {
        viewModel.deleteAllSni();

        Toast.makeText(this, "All loaded SNI have been deleted.", Toast.LENGTH_SHORT).show();
    }

    private void onLoadButtonClicked() {
        // Create an intent to open the file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // We are looking for any kind of file, but you could restrict it,
        // for example, to "text/plain" for text files.
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            // Launch the intent using the ActivityResultLauncher
            filePickerLauncher.launch(Intent.createChooser(intent, "Select a SNI file"));
        } catch (ActivityNotFoundException ex) {
            // Potentially handle the case where the device has no file manager
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readFileContent(Uri uri) {
        List<SniEntity> sniList = new ArrayList<>();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Trim whitespace and ignore empty or commented lines
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                    sniList.add(SniEntity.builder()
                            .sni(trimmedLine)
                            .checked(false)
                            .build());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading SNI file", e);
            Toast.makeText(this, "Error: Could not read the file.", Toast.LENGTH_SHORT).show();
        }

        if (!sniList.isEmpty()) {
            Futures.addCallback(viewModel.insertAllSni(sniList), new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully inserted " + sniList.size() + " SNIs into the database.");
                    Toast.makeText(BypassMethodsActivity.this, "Loaded " + sniList.size() + " SNI from file.", Toast.LENGTH_LONG).show();

                    viewModel.refreshSniCount();
                }

                @Override
                public void onFailure(Throwable t) {
                    //todo: add error message
                }
            }, getMainExecutor());

        } else {
            Log.d(TAG, "No valid SNIs found in the selected file.");
            Toast.makeText(this, "File is empty or contains no valid SNI entries.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onEditSNIServer(View view) {
        View inflated = View.inflate(this, R.layout.sni_dialog_layout, null);
        TextInputEditText sniEditText = inflated.findViewById(R.id.text_edit_sni);
        sniEditText.setText(viewModel.getCurrentSni());

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(inflated);
        alertDialogBuilder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: save_button");
            Optional.ofNullable(sniEditText.getText())
                    .map(Object::toString)
                    .ifPresent(viewModel::validateAndSetSni);
        });
        alertDialogBuilder.setNeutralButton(getString(R.string.reset_default_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: reset_default_button");
            viewModel.resetToDefault();
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: cancel_button");
        });
        alertDialogBuilder.show();
    }
}
