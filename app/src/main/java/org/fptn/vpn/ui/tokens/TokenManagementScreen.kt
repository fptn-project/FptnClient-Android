package org.fptn.vpn.ui.tokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.TokenEntity
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.MultilineTextInputDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpdateToken: () -> Unit,
    viewModel: TokenManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokens by viewModel.tokensLiveData.observeAsState(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var tokenToDelete by remember { mutableStateOf<TokenEntity?>(null) }

    // State for the error dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorType by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_key_24),
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.token_management_title),
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(tokens) { index, token ->
                    val visibleCount = tokens.count { it.isVisible }
                    val isLastVisibleToken = token.isVisible && visibleCount == 1

                    TokenCard(
                        token = token,
                        number = if (tokens.size > 1) index + 1 else null,
                        isLastToken = tokens.size == 1,
                        isLastVisibleToken = isLastVisibleToken,
                        onVisibilityChange = { isVisible ->
                            viewModel.setTokenVisible(token, isVisible)
                        },
                        onDeleteClick = {
                            tokenToDelete = token
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LegacyPillButton(
                text = stringResource(R.string.token_add),
                backgroundDrawable = R.drawable.round_back_secondary_100,
                textColor = Primary,
                bold = true,
                contentPadding = 12.dp,
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }

    if (showAddDialog) {
        val dialogDensity = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides dialogDensity) {
            MultilineTextInputDialog(
                title = stringResource(R.string.token_add),
                hint = stringResource(R.string.token_add_hint),
                initialText = "",
                onSave = { tokenStr ->
                    viewModel.addToken(tokenStr) { success, error ->
                        if (!success) {
                            errorType = error
                            showErrorDialog = true
                        }
                    }
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }

    if (showErrorDialog) {
        val dialogDensity = LocalDensity.current
        val errorMessage = when (errorType) {
            "TOKEN_EXISTS" -> context.getString(R.string.token_error_exists)
            "INVALID_TOKEN" -> context.getString(R.string.token_error_invalid)
            else -> context.getString(R.string.token_error_unknown)
        }

        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        text = stringResource(R.string.token_error_title),
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        text = errorMessage,
                        color = White,
                        fontSize = 14.sp,
                        lineHeight = 17.sp // Single line spacing
                    )
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    if (errorType == "TOKEN_EXISTS") {
                        TextButton(onClick = {
                            showErrorDialog = false
                            onNavigateToUpdateToken()
                        }) {
                            Text(stringResource(R.string.update_token_button))
                        }
                    } else {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    if (errorType == "TOKEN_EXISTS") {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text(stringResource(R.string.cancel_button))
                        }
                    }
                }
            }
        )
    }

    tokenToDelete?.let { token ->
        val dialogDensity = LocalDensity.current
        AlertDialog(
            onDismissRequest = { tokenToDelete = null },
            title = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(stringResource(R.string.token_delete_title), color = White)
                }
            },
            text = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    Text(
                        text = String.format(stringResource(R.string.token_delete_confirmation), token.serviceName),
                        color = White,
                        lineHeight = 17.sp,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = {
                        viewModel.deleteToken(token)
                        tokenToDelete = null
                    }) {
                        Text(stringResource(R.string.token_delete))
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides dialogDensity) {
                    TextButton(onClick = { tokenToDelete = null }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            }
        )
    }
}

@Composable
private fun TokenCard(
    token: TokenEntity,
    number: Int?,
    isLastToken: Boolean,
    isLastVisibleToken: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (number != null) "$number. ${token.serviceName}" else token.serviceName,
                        color = White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = token.username,
                        color = White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = token.isVisible,
                    onCheckedChange = onVisibilityChange,
                    enabled = !isLastVisibleToken
                )
            }

            val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
            Text(
                text = String.format(stringResource(R.string.token_updated_at), dateFormat.format(Date(token.updatedAt))),
                color = White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val isDeleteDisabled = isLastToken || isLastVisibleToken
                Text(
                    text = stringResource(R.string.token_delete),
                    color = if (isDeleteDisabled) White.copy(alpha = 0.3f) else White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(enabled = !isDeleteDisabled, onClick = onDeleteClick)
                        .padding(8.dp)
                )
            }
        }
    }
}