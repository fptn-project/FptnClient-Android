package org.fptn.vpn.ui.updatetoken

import android.app.Activity
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.R
import org.fptn.vpn.ui.MainActivity
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.Black
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.Yellow
import org.fptn.vpn.views.updatetoken.UpdateTokenViewModel

private const val TAG = "UpdateTokenScreen"
private val TOKEN_CLIPBOARD_PREFIXES = listOf("fptn:", "fptnb:")

/**
 * Compose port of the legacy `UpdateTokenActivity` / `settings_layout_update_token.xml`.
 * Reuses [UpdateTokenViewModel] unchanged. Home and Settings aren't ported yet, so every
 * exit from this screen bridges to them via a plain legacy `Intent`, exactly like the
 * Activity it replaces.
 */
@Composable
fun UpdateTokenScreen(
    viewModel: UpdateTokenViewModel = viewModel(),
) {
    val context = LocalContext.current
    val errorText by viewModel.errorTextLiveData.observeAsState("")
    var tokenText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var showShareDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

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

    fun goToSettingsAndFinish() {
        context.startActivity(MainActivity.intentForRoute(context, AppRoute.SETTINGS))
        (context as? Activity)?.finish()
    }

    fun onCancel() = goToSettingsAndFinish()

    fun onSave() {
        try {
            val updateResult = viewModel.parseAndSaveToken(tokenText.text)
            Futures.addCallback(
                updateResult,
                object : FutureCallback<Void?> {
                    override fun onSuccess(result: Void?) {
                        Toast.makeText(context, R.string.token_was_updated, Toast.LENGTH_SHORT).show()
                        goToSettingsAndFinish()
                    }

                    override fun onFailure(t: Throwable) {
                        XLog.tag(TAG).e("Token update failed: %s", t.message)
                        Toast.makeText(context, t.message.orEmpty(), Toast.LENGTH_SHORT).show()
                        viewModel.errorTextLiveData.postValue(t.message)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        } catch (e: Exception) {
            XLog.tag(TAG).e("Token parsing failed: %s", e.message)
            Toast.makeText(context, R.string.token_saving_failed, Toast.LENGTH_SHORT).show()
            viewModel.errorTextLiveData.postValue(context.getString(R.string.token_saving_failed))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.icon_settings_circle_100),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .size(80.dp),
            )

            HtmlLinkText(
                html = stringResource(R.string.settings_token_info_html),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                LegacyPillButton(
                    text = stringResource(R.string.cancel_button),
                    backgroundDrawable = R.drawable.round_back_secondary_cancel_100,
                    textColor = Primary,
                    onClick = ::onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                LegacyPillButton(
                    text = stringResource(R.string.save_button),
                    backgroundDrawable = R.drawable.round_back_secondary_100,
                    textColor = Primary,
                    onClick = ::onSave,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
            }

            Text(
                text = errorText,
                color = Yellow,
                modifier = Modifier.padding(start = 5.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.HOME)) },
            onNavigateSettings = { context.startActivity(MainActivity.intentForRoute(context, AppRoute.SETTINGS)) },
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}
