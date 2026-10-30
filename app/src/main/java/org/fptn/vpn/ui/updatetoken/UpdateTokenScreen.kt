package org.fptn.vpn.ui.updatetoken

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import kotlinx.coroutines.launch
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Black
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.Yellow
import org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName
import org.fptn.vpn.vpnclient.exception.ErrorCode
import org.fptn.vpn.vpnclient.exception.PVNClientException

private const val TAG = "UpdateTokenScreen"
private val TOKEN_CLIPBOARD_PREFIXES = listOf("fptn:", "fptnb:")

@Composable
fun UpdateTokenScreen(
    onNavigateHome: () -> Unit,
    onDoneNavigateSettings: () -> Unit,
    onNavigateToTokenManagement: () -> Unit,
    viewModel: UpdateTokenViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val errorText by viewModel.errorTextLiveData.observeAsState("")
    var tokenText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var showShareDialog by remember { mutableStateOf(false) }
    var showNotFoundError by remember { mutableStateOf(false) }
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

    fun onCancel() = onDoneNavigateSettings()

    fun onSave() {
        // Do not save if the field is empty
        if (tokenText.text.isBlank()) {
            return
        }
        coroutineScope.launch {
            try {
                val isUpdated = viewModel.parseAndSaveToken(tokenText.text)
                if (isUpdated) {
                    Toast.makeText(context, R.string.token_was_updated, Toast.LENGTH_SHORT).show()
                    onDoneNavigateSettings()
                } else {
                    showNotFoundError = true
                }
            } catch (e: Exception) {
                XLog.tag(TAG).e("Token update failed: %s", e.message)
                val localizedMessage = if (e is PVNClientException && e.errorCode != ErrorCode.UNKNOWN_ERROR) {
                    getStringResourceByName(context, e.errorCode.value) ?: e.message.orEmpty()
                } else {
                    e.message.orEmpty()
                }
                // Display the error only below the field (without a Toast)
                viewModel.errorTextLiveData.postValue(localizedMessage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
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
                    .heightIn(min = 48.dp, max = 140.dp)
                    .padding(top = 16.dp)
                    .legacyDrawableBackground(R.drawable.round_back_white10_20),
            ) {
                BasicTextField(
                    value = tokenText,
                    onValueChange = {
                        tokenText = it
                        // Clear the error when typing
                        if (errorText.isNotEmpty()) {
                            viewModel.errorTextLiveData.postValue("")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 76.dp),
                    textStyle = TextStyle(color = Black, fontSize = 16.sp),
                    singleLine = false,
                    maxLines = 5,
                    cursorBrush = SolidColor(Black),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
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
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 8.dp),
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
            onNavigateHome = onNavigateHome,
            onNavigateSettings = onDoneNavigateSettings,
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }

    if (showNotFoundError) {
        val dialogDensity = LocalDensity.current
        AlertDialog(
            onDismissRequest = { showNotFoundError = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.token_error_title), color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        text = stringResource(R.string.token_error_not_found),
                        color = White,
                        fontSize = 14.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        showNotFoundError = false
                        onNavigateToTokenManagement()
                    }) {
                        Text(stringResource(R.string.token_management_title))
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = { showNotFoundError = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            }
        )
    }
}