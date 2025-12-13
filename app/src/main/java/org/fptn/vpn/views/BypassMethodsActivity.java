package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.Optional;

public class BypassMethodsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private MutableLiveData<String> SNIMutableLiveData;
    private View sniLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_bypass_methods_layout);

        SNIMutableLiveData = new MutableLiveData<>(getApplication().getString(R.string.default_sni));
        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuSettings));

        // Initialize elements
        sniLayout = findViewById(R.id.sni_layout);
        View editSniButton = findViewById(R.id.imageView);

        // Setup RadioGroup
        RadioGroup protocolRadioGroup = findViewById(R.id.bypass_method_radio_button_group);
        RadioButton sniSpoofingRadioButton = findViewById(R.id.sni_spoofing_radio_button);
        RadioButton obfuscationRadioButton = findViewById(R.id.obfuscation_radio_button);
        RadioButton sniRealityRadioButton = findViewById(R.id.sni_reality_radio_button);

        // Set initial state
        BypassCensorshipMethod currentMethod = SharedPrefUtils.getBypassCensorshipMethod(this);

        switch (currentMethod) {
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

        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sni_spoofing_radio_button) {
                Log.d(TAG, "Selected SNI spoofing");
                SharedPrefUtils.saveBypassCensorshipMethod(this, BypassCensorshipMethod.SNI_SPOOFING);
                showView(sniLayout);
            } else if (checkedId == R.id.obfuscation_radio_button) {
                Log.d(TAG, "Selected TLS obfuscation");
                SharedPrefUtils.saveBypassCensorshipMethod(this, BypassCensorshipMethod.TLS_OBFUSCATION);
                hideView(sniLayout);
            } else if (checkedId == R.id.sni_reality_radio_button) {
                Log.d(TAG, "Selected SNI Reality");
                SharedPrefUtils.saveBypassCensorshipMethod(this, BypassCensorshipMethod.SNI_REALITY);
                showView(sniLayout);
            }
        });

        // SNI field
        TextView sniTextField = findViewById(R.id.SNI_text_field);
        SNIMutableLiveData.observe(this, sniTextField::setText);
        SNIMutableLiveData.postValue(SharedPrefUtils.getSniHostname(this));

        // Click handlers
        sniLayout.setOnClickListener(this::onEditSNIServer);
        editSniButton.setOnClickListener(this::onEditSNIServer);

        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        Button saveButton = findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            finish();
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            finish();
        });
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

    public void onEditSNIServer(View view) {
        View inflated = View.inflate(this, R.layout.sni_dialog_layout, null);
        TextInputEditText sniEditText = inflated.findViewById(R.id.text_edit_sni);
        sniEditText.setText(SNIMutableLiveData.getValue());

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(inflated);
        alertDialogBuilder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: save_button");
            Optional.ofNullable(sniEditText.getText())
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .ifPresent(newSni -> {
                        Log.d(TAG, "new SNI: " + newSni);
                        SharedPrefUtils.saveSniHostname(this, newSni);
                        SNIMutableLiveData.postValue(newSni);
                    });
        });
        alertDialogBuilder.setNeutralButton(getString(R.string.reset_default_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: reset_default_button");
            SharedPrefUtils.resetToDefaultSniHostname(this);
            SNIMutableLiveData.postValue(SharedPrefUtils.getSniHostname(this));
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: cancel_button");
        });
        alertDialogBuilder.show();
    }
}
