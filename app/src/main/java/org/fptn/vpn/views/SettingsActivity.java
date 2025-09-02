package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.views.adapter.FptnServerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Optional;

import lombok.Getter;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private ListView serverListView;

    private MutableLiveData<String> SNIMutableLiveData;

    @Getter
    private FptnServerViewModel fptnViewModel;

    private SwitchCompat permissionShowNotificationButton;
    private SwitchCompat permissionBatteryOptimizationButton;
    private SwitchCompat permissionBackgroundDataTransferButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        SNIMutableLiveData = new MutableLiveData<>(getApplication().getString(R.string.default_sni));

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuSettings));

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getServerDtoListLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                serverListView.setAdapter(new FptnServerAdapter(fptnServerDtos, R.layout.settings_server_list_item)); // NEED TO CHANGE THE ITEM LAYOUT
                setListViewHeightBasedOnChildren(serverListView);
            } else {
                // goto Login activity
                Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
        serverListView = findViewById(R.id.settings_servers_list);

        TextView versionTextView = findViewById(R.id.settings_fptn_version);
        try {
            Context context = getApplicationContext();
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            final String version = pInfo.versionName;
            versionTextView.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // about
        TextView about = findViewById(R.id.settings_about);
        about.setText(Html.fromHtml(getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        // token's info
        TextView tokenInfo = findViewById(R.id.settings_token_info_html);
        tokenInfo.setText(Html.fromHtml(getString(R.string.settings_token_info_html), Html.FROM_HTML_MODE_LEGACY));
        tokenInfo.setMovementMethod(LinkMovementMethod.getInstance());

        // SNI field
        TextView sniTextField = findViewById(R.id.SNI_text_field);
        SNIMutableLiveData.observe(this, sniTextField::setText);
        SNIMutableLiveData.postValue(SharedPrefUtils.getSniHostname(this));

        // Permission settings
        permissionShowNotificationButton = findViewById(R.id.permission_show_notification_button);
        permissionShowNotificationButton.setOnClickListener(view -> requestNotificationPermission());

        permissionBatteryOptimizationButton = findViewById(R.id.permission_battery_optimization_button);
        permissionBatteryOptimizationButton.setOnClickListener(view -> requestBatteryOptimisationPermission());

        permissionBackgroundDataTransferButton = findViewById(R.id.permission_background_data_transfer_button);
        permissionBackgroundDataTransferButton.setOnClickListener(view -> requestBackgroundDataTransferPermission());

        // Our sponsors
        TextView textView = findViewById(R.id.sponsors_list);
        textView.setText(Html.fromHtml(getString(R.string.sponsors_usernames)));
    }

    @Override
    protected void onResume() {
        super.onResume();

        setPermissionButtonState(PermissionsUtils.checkNotificationPermission(this), permissionShowNotificationButton);
        setPermissionButtonState(PermissionsUtils.checkBatteryOptimizations(this), permissionBatteryOptimizationButton);
        setPermissionButtonState(PermissionsUtils.checkBackgroundDataTransferRestrictions(this), permissionBackgroundDataTransferButton);
    }

    private void setPermissionButtonState(boolean isGranted, SwitchCompat switchView) {
        switchView.setEnabled(true);
        if (isGranted) {
            switchView.setClickable(false);
            switchView.setChecked(true);
        } else {
            switchView.setClickable(true);
            switchView.setChecked(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        // Permission is not granted, show a dialog to explain reason
        new AlertDialog.Builder(this)
                .setTitle(R.string.notifications_request_title)
                .setMessage(R.string.notifications_request_reason)
                .setPositiveButton(R.string.grant, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setOnDismissListener(v -> permissionShowNotificationButton.setChecked(false))
                .create()
                .show();
    }

    private void requestBatteryOptimisationPermission() {
        new AlertDialog.Builder(this)
                // todo: add in settings show all needed restrictions granted?
                .setTitle(getString(R.string.battery_optimization_request_dialog_title))
                .setMessage(getString(R.string.battery_optimization_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Battery optimisation permission denied!");
                    permissionBatteryOptimizationButton.setChecked(false);
                })
                .show();
    }

    private void requestBackgroundDataTransferPermission() {
        /* If somebody worry about low speed in background - disable restriction on network transfer data*/
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.background_data_request_dialog_title))
                .setMessage(getString(R.string.background_data_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Background data transfer permission denied!");
                    permissionBackgroundDataTransferButton.setChecked(false);
                })
                .show();
    }


    public void onLogout(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    fptnViewModel.deleteAll();
                    // goto Login activity
                    Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // called from settings_layout.xml
    public void onUpdateToken(View v) {
        // Goto update token
        Intent intent = new Intent(SettingsActivity.this, SettingsActivityUpdateToken.class);
        startActivity(intent);
    }

    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public void onEditSNIServer(View view) {
        View inflated = View.inflate(this, R.layout.sni_dialog_layout, null);
        TextInputEditText sniEditText = inflated.findViewById(R.id.text_edit_sni);
        SNIMutableLiveData.observe(this, sniEditText::setText);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(inflated);
        alertDialogBuilder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: save_button");
            Optional.ofNullable(sniEditText.getText())
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .ifPresent(newSni -> {
                        //todo: add validation?
                        Log.d(TAG, "new SNI: " + newSni);
                        SharedPrefUtils.saveSniHostname(this, newSni);
                        SNIMutableLiveData.postValue(newSni);
                    });
        });
        alertDialogBuilder.setNeutralButton(getString(R.string.reset_default_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: reset_default_button");
            SharedPrefUtils.resetToDefaultSniHostname(this);
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: cancel_button");
        });
        alertDialogBuilder.show();
    }
}
