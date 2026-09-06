package org.fptn.vpn.ui.login

import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import androidx.core.content.ContextCompat
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Black
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.Yellow

private const val TAG = "LoginScreen"
private val TOKEN_CLIPBOARD_PREFIXES = listOf("fptn:", "fptnb:")

/**
 * Compose port of the legacy `LoginActivity` / `login_layout.xml`. Reuses
 * [LoginActivityViewModel] unchanged; only the view layer is new.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginActivityViewModel = viewModel(),
) {
    val context = LocalContext.current
    val errorText by viewModel.errorTextLiveData.observeAsState("")
    var tokenText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // The legacy field never shows the soft keyboard (it's meant to be pasted into), so hide it
    // the moment focus lands here, matching `setShowSoftInputOnFocus(false)`.
    LaunchedEffect(isFocused) {
        if (isFocused) keyboardController?.hide()
    }

    fun onPaste() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.text?.toString()?.trim()
        if (text != null && TOKEN_CLIPBOARD_PREFIXES.any { text.startsWith(it) }) {
            tokenText = TextFieldValue(text, selection = TextRange(text.length))
        }
    }

    fun onLogin() {
        try {
            val updateResult = viewModel.parseAndSaveToken(tokenText.text)
            Futures.addCallback(
                updateResult,
                object : FutureCallback<Void?> {
                    override fun onSuccess(result: Void?) {
                        onLoginSuccess()
                    }

                    override fun onFailure(t: Throwable) {
                        XLog.tag(TAG).e("Login failed: %s", t.message)
                        Toast.makeText(context, t.message.orEmpty(), Toast.LENGTH_SHORT).show()
                        viewModel.errorTextLiveData.postValue(t.message)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        } catch (e: Exception) {
            XLog.tag(TAG).e("Token parsing failed at login: %s", e.message)
            Toast.makeText(context, R.string.token_saving_failed, Toast.LENGTH_SHORT).show()
            viewModel.errorTextLiveData.postValue(context.getString(R.string.token_saving_failed))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.icon_account_circle_100),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )

        HtmlLinkText(
            html = stringResource(R.string.telegram_bot_html),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            color = White,
            fontSize = 16.sp,
            padding = PaddingValues(8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 16.dp)
                .legacyDrawableBackground(R.drawable.round_back_white10_20),
        ) {
            BasicTextField(
                value = tokenText,
                onValueChange = { tokenText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 76.dp),
                textStyle = TextStyle(color = Black, fontSize = 16.sp),
                singleLine = true,
                cursorBrush = SolidColor(Black),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (tokenText.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.paste_your_token),
                                color = Hint,
                                fontSize = 16.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_content_paste_24),
                    contentDescription = stringResource(R.string.paste_button),
                    tint = Primary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onPaste() },
                )
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_close_24),
                    contentDescription = stringResource(R.string.clear_button),
                    tint = Primary,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(24.dp)
                        .clickable { tokenText = TextFieldValue("") },
                )
            }
        }

        LegacyPillButton(
            text = stringResource(R.string.login_button),
            backgroundDrawable = R.drawable.round_back_secondary_100,
            textColor = Primary,
            onClick = ::onLogin,
            modifier = Modifier.padding(top = 32.dp),
        )

        Text(
            text = errorText,
            color = Yellow,
            modifier = Modifier.padding(start = 5.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
        )
    }
}
