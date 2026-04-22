package org.fptn.vpn.views.bypassmethod;

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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.utils.ViewUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;

import java.util.Optional;

public class BypassMethodsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private View sniLayout;

    private BypassMethodsViewModel viewModel;

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
            if (checkedId == R.id.obfuscation_radio_button) {
                Log.d(TAG, "Selected TLS obfuscation");
                viewModel.setBypassMethod(BypassCensorshipMethod.TLS_OBFUSCATION);
            }
            /* Chrome */
            else if (checkedId == R.id.sni_reality_radio_button_chrome_147) {
                Log.d(TAG, "Selected SNI Reality Chrome 147");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_CHROME_147);
            } else if (checkedId == R.id.sni_reality_radio_button_chrome_146) {
                Log.d(TAG, "Selected SNI Reality Chrome 146");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_CHROME_146);
            } else if (checkedId == R.id.sni_reality_radio_button_chrome_145) {
                Log.d(TAG, "Selected SNI Reality Chrome 145");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_CHROME_145);
            }
            /* Firefox */
            else if (checkedId == R.id.sni_reality_radio_button_firefox_149) {
                Log.d(TAG, "Selected SNI Reality Firefox 149");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_FIREFOX_149);
            }
            /* Yandex */
            else if (checkedId == R.id.sni_reality_radio_button_yandex_26) {
                Log.d(TAG, "Selected SNI Reality Yandex 26");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_YANDEX_26);
            } else if (checkedId == R.id.sni_reality_radio_button_yandex_25) {
                Log.d(TAG, "Selected SNI Reality Yandex 25");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_YANDEX_25);
            } else if (checkedId == R.id.sni_reality_radio_button_yandex_24) {
                Log.d(TAG, "Selected SNI Reality Yandex 24");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_YANDEX_24);
            }
            /* Safari */
            else if (checkedId == R.id.sni_reality_radio_button_safari_26) {
                Log.d(TAG, "Selected SNI Reality Safari 26");
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_SAFARI_26);
            } else {
                // default
                viewModel.setBypassMethod(BypassCensorshipMethod.SNI_REALITY_YANDEX_25);
            }
        });

        RadioButton obfuscationRadioButton = findViewById(R.id.obfuscation_radio_button);

        RadioButton sniRealityRadioButtonChrome147 = findViewById(R.id.sni_reality_radio_button_chrome_147);
        RadioButton sniRealityRadioButtonChrome146 = findViewById(R.id.sni_reality_radio_button_chrome_146);
        RadioButton sniRealityRadioButtonChrome145 = findViewById(R.id.sni_reality_radio_button_chrome_145);

        RadioButton sniRealityRadioButtonFirefox149 = findViewById(R.id.sni_reality_radio_button_firefox_149);

        RadioButton sniRealityRadioButtonYandex26 = findViewById(R.id.sni_reality_radio_button_yandex_26);
        RadioButton sniRealityRadioButtonYandex25 = findViewById(R.id.sni_reality_radio_button_yandex_25);
        RadioButton sniRealityRadioButtonYandex24 = findViewById(R.id.sni_reality_radio_button_yandex_24);

        RadioButton sniRealityRadioButtonSafari26 = findViewById(R.id.sni_reality_radio_button_safari_26);

        viewModel.getBypassCensorshipMethodMutableLiveData().observe(this, bypassCensorshipMethod -> {
            switch (bypassCensorshipMethod) {
                case TLS_OBFUSCATION:
                    obfuscationRadioButton.setChecked(true);
                    ViewUtils.hideView(sniLayout);
                    break;
                case SNI_SPOOFING:  // deprecated
                case SNI_REALITY:  // deprecated
                /* Yandex */
                case SNI_REALITY_YANDEX_25:
                    sniRealityRadioButtonYandex25.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                case SNI_REALITY_YANDEX_26:
                    sniRealityRadioButtonYandex26.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                case SNI_REALITY_YANDEX_24:
                    sniRealityRadioButtonYandex24.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                /* Chrome */
                case SNI_REALITY_CHROME_147:
                    sniRealityRadioButtonChrome147.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                case SNI_REALITY_CHROME_146:
                    sniRealityRadioButtonChrome146.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                case SNI_REALITY_CHROME_145:
                    sniRealityRadioButtonChrome145.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                /* Firefox */
                case SNI_REALITY_FIREFOX_149:
                    sniRealityRadioButtonFirefox149.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                /* Safari */
                case SNI_REALITY_SAFARI_26:
                    sniRealityRadioButtonSafari26.setChecked(true);
                    ViewUtils.showView(sniLayout);
                    break;
                default:
                    sniRealityRadioButtonYandex25.setChecked(true);
                    ViewUtils.showView(sniLayout);
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
