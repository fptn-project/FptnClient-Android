package org.fptn.vpn.ui.logs

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import java.io.File

private const val LOAD_MORE_INDEX_THRESHOLD = 2

/**
 * Compose port of the legacy `LogsActivity` / `logs_layout.xml`, extended into a `kubectl logs`
 * style pager: opens on the newest lines, scrolling up fetches further history a page at a time,
 * and scrolling back down to the end picks up whatever was appended since (see [LogsViewModel]).
 */
@Composable
fun LogsScreen(
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: LogsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiStateLiveData.observeAsState(LogsUiState.Loading)
    var showShareDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var pendingPrependAnchor by remember { mutableStateOf<PrependAnchor?>(null) }
    var didInitialScroll by remember { mutableStateOf(false) }

    val content = uiState as? LogsUiState.Content

    LaunchedEffect(content?.lines) {
        val lines = content?.lines ?: return@LaunchedEffect
        val anchor = pendingPrependAnchor
        if (anchor != null && lines.size > anchor.lineCountBefore) {
            val inserted = lines.size - anchor.lineCountBefore
            listState.scrollToItem(anchor.indexBefore + inserted, anchor.offsetBefore)
            pendingPrependAnchor = null
        } else if (!didInitialScroll && lines.isNotEmpty()) {
            listState.scrollToItem(lines.lastIndex)
            didInitialScroll = true
        }
    }

    // Scrolled near the top: page in older history. Reads the ViewModel's live state directly
    // (rather than closing over `content`) since this effect is launched once and then loops.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val current = viewModel.uiStateLiveData.value as? LogsUiState.Content ?: return@collect
            if (index <= LOAD_MORE_INDEX_THRESHOLD && current.hasMoreBefore && !current.loadingOlder) {
                pendingPrependAnchor = PrependAnchor(
                    indexBefore = index,
                    offsetBefore = listState.firstVisibleItemScrollOffset,
                    lineCountBefore = current.lines.size,
                )
                viewModel.loadOlder()
            }
        }
    }

    // Scrolled to the end: check for anything appended to the file since it was last read.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1
        }.collect { atBottom -> if (atBottom) viewModel.loadNewer() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background)
            // Excludes the bottom side deliberately: BottomNavBar consumes the navigation-bar
            // inset itself so its background reaches the real screen edge instead of floating
            // above it with the screen background showing through underneath (see
            // BottomNavBar's window insets listener).
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_24),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 30.dp)
                .size(80.dp),
        )

        Text(
            text = stringResource(R.string.logs),
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 8.dp),
        )

        val centeredModifier = Modifier.weight(1f)
        when (val state = uiState) {
            LogsUiState.Loading -> CenteredMessage(centeredModifier) { CircularProgressIndicator(color = White) }
            LogsUiState.NoLogsDirectory ->
                CenteredMessage(centeredModifier) { PlaceholderText(stringResource(R.string.logs_error_no_directory)) }
            LogsUiState.NoLogFiles ->
                CenteredMessage(centeredModifier) { PlaceholderText(stringResource(R.string.logs_error_no_files)) }
            is LogsUiState.ReadError ->
                CenteredMessage(centeredModifier) {
                    PlaceholderText(stringResource(R.string.logs_error_read_failed, state.message ?: ""))
                }
            is LogsUiState.Content ->
                if (state.lines.isEmpty()) {
                    CenteredMessage(centeredModifier) { PlaceholderText(stringResource(R.string.logs_error_empty_file)) }
                } else {
                    val defaultToolbar = LocalTextToolbar.current
                    val toastingToolbar = remember(defaultToolbar) {
                        ToastOnCopyTextToolbar(defaultToolbar) {
                            Toast.makeText(context, R.string.logs_copied, Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Logs are technical content. Their font size must remain fixed and must not depend
                    // on either the system fontScale or our custom fontScale override in Experimental Settings.
                    // Override LocalDensity with fontScale = 1f.
                    val logsDensity = LocalDensity.current
                    val logsDensityFixed = Density(
                        density = logsDensity.density,
                        fontScale = 1f,
                    )

                    CompositionLocalProvider(
                        LocalTextToolbar provides toastingToolbar,
                        LocalDensity provides logsDensityFixed,
                    ) {
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color(0xFF202020)),
                        ) {
                            LazyColumn(state = listState) {
                                if (state.hasMoreBefore) {
                                    item(key = "loading_older") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (state.loadingOlder) {
                                                CircularProgressIndicator(color = White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                items(state.lines) { line ->
                                    Text(
                                        text = line,
                                        color = Color(0xFFE0E0E0),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
        }

        LegacyPillButton(
            text = stringResource(R.string.send_log_file),
            backgroundDrawable = R.drawable.round_back_secondary_100,
            textColor = Primary,
            onClick = { sendLogFile(context, content?.file) },
            enabled = content?.file != null,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = onNavigateHome,
            onNavigateSettings = onNavigateSettings,
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

private data class PrependAnchor(val indexBefore: Int, val offsetBefore: Int, val lineCountBefore: Int)

/**
 * Delegates to the platform [TextToolbar] so text selection/copy keeps its normal behavior,
 * but fires [onCopy] after the selected text is actually copied to the clipboard.
 */
private class ToastOnCopyTextToolbar(
    private val delegate: TextToolbar,
    private val onCopy: () -> Unit,
) : TextToolbar {
    override val status: TextToolbarStatus get() = delegate.status

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        delegate.showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested?.let {
                {
                    it()
                    onCopy()
                }
            },
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
        )
    }

    override fun hide() = delegate.hide()
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlaceholderText(text: String) {
    Text(
        text = text,
        color = White,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

private fun sendLogFile(context: Context, file: File?) {
    if (file == null) {
        Toast.makeText(context, R.string.logs_empty, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_log_file)))
}
