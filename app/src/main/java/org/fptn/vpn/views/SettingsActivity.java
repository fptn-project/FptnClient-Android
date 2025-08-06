package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.views.adapter.FptnServerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Optional;

import lombok.Getter;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private ListView serverListView;

    private MutableLiveData<String> SNIMutableLiveData;

    @Getter
    private FptnServerViewModel fptnViewModel;

    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        SNIMutableLiveData = new MutableLiveData<>(getApplication().getString(R.string.default_sni));

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        // FIXME
        bottomNavigationView = findViewById(R.id.bottomNavBar);
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

        // Switch reconnect on change ip
        boolean isEnabledReconnectOnChangeIp = SharedPrefUtils.getReconnectEnable(this);
        SwitchCompat switchCompat = findViewById(R.id.toggle_reconnect_on_change_ip);
        switchCompat.setChecked(isEnabledReconnectOnChangeIp);
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPrefUtils.saveReconnectEnable(this, isChecked));

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
            SharedPrefUtils.saveSniHostname(this, getString(R.string.default_sni));
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> Log.d(TAG, "onEditSNIServer: cancel_button"));
        alertDialogBuilder.show();
    }
}
