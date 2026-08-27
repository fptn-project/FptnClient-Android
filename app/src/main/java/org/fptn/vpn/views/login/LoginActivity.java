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

package org.fptn.vpn.views.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elvishew.xlog.XLog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.views.home.HomeActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private LoginActivityViewModel viewModel;
    private EditText linkEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);

        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.telegram_bot_html), Html.FROM_HTML_MODE_LEGACY));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        TextView errorTextView = findViewById(R.id.errorTextView);
        viewModel.getErrorTextLiveData().observe(this, errorTextView::setText);

        linkEditText = findViewById(R.id.fptn_login_link_input);

        Button loginButton = findViewById(R.id.fptn_login_button);
        loginButton.setOnClickListener((v) -> onLogin());

        ImageView pasteIcon = findViewById(R.id.fptn_paste_icon);
        pasteIcon.setOnClickListener((v) -> onPaste());

        ImageView clearIcon = findViewById(R.id.fptn_clear_icon);
        clearIcon.setOnClickListener((v) -> linkEditText.setText(""));

        // hide keyboard
        linkEditText.setTextIsSelectable(true);
        linkEditText.setShowSoftInputOnFocus(false);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts

        // miss back button
        getOnBackPressedDispatcher().addCallback(this, new MyOnBackPressedCallback());
    }

    public void onLogin() {
        final String tokenLink = linkEditText.getText().toString();
        try {
            ListenableFuture<Void> updateResult = viewModel.parseAndSaveToken(tokenLink);
            Futures.addCallback(updateResult, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);

                    finish();
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Login failed: %s", t.getMessage());
                    Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    viewModel.getErrorTextLiveData().postValue(t.getMessage());
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            XLog.tag(TAG).e("Token parsing failed at login: %s", e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();
            viewModel.getErrorTextLiveData().postValue(getString(R.string.token_saving_failed));
        }
    }

    private void onPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                linkEditText.setText(item.getText().toString());
                linkEditText.setSelection(linkEditText.getText().length());
            }
        }
    }

    private static class MyOnBackPressedCallback extends OnBackPressedCallback {
        public MyOnBackPressedCallback() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {

        }
    }
}