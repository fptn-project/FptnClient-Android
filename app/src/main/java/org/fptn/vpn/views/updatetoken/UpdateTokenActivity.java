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

package org.fptn.vpn.views.updatetoken;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.settings.SettingsActivity;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import lombok.Getter;

public class UpdateTokenActivity extends AppCompatActivity {
    private static final String TAG = UpdateTokenActivity.class.getSimpleName();

    @Getter
    private UpdateTokenViewModel viewModel;
    private EditText linkEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout_update_token);

        initializeVariable();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(UpdateTokenViewModel.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        CustomBottomNavigationListener bottomNavigationListener = new CustomBottomNavigationListener(this, R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(bottomNavigationListener);
        bottomNavigationView.setOnItemReselectedListener(bottomNavigationListener);

        TextView errorTextView = findViewById(R.id.errorTextView);
        viewModel.getErrorTextLiveData().observe(this, errorTextView::setText);

        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.settings_token_info_html), Html.FROM_HTML_MODE_LEGACY));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        linkEditText = findViewById(R.id.fptn_login_link_input);

        linkEditText.setOnClickListener((v) -> onPaste());

        ImageView pasteIcon = findViewById(R.id.fptn_paste_icon);
        pasteIcon.setOnClickListener((v) -> onPaste());

        ImageView clearIcon = findViewById(R.id.fptn_clear_icon);
        clearIcon.setOnClickListener((v) -> linkEditText.setText(""));

        // HIDE KEYBOARD
        linkEditText.setTextIsSelectable(true);
        linkEditText.setShowSoftInputOnFocus(false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts
    }

    private void onPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                String text = item.getText().toString().trim();
                if (text.startsWith("fptn:") || text.startsWith("fptnb:")) {
                    linkEditText.setText(text);
                    linkEditText.setSelection(linkEditText.getText().length());
                }
            }
        }
    }

    public void onCancel(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        finish();
    }

    public void onSave(View v) {
        final String tokenLink = linkEditText.getText().toString();

        try {
            ListenableFuture<Void> updateResult = viewModel.parseAndSaveToken(tokenLink);
            Futures.addCallback(updateResult, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getApplicationContext(), R.string.token_was_updated, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(UpdateTokenActivity.this, SettingsActivity.class);
                    startActivity(intent);

                    finish();
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Token update failed: %s", t.getMessage());
                    Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();

                    viewModel.getErrorTextLiveData().postValue(t.getMessage());
                }

            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            XLog.tag(TAG).e("Token parsing failed: %s", e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();

            viewModel.getErrorTextLiveData().postValue(getString(R.string.token_saving_failed));
        }
    }

}
