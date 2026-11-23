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

package org.fptn.vpn.ui.common

import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.White
import java.io.FileNotFoundException
import java.io.IOException

/**
 * File-level errors during token import from a .txt file.
 *
 * NOTE: Token-content errors (invalid format, missing fields, etc.) are NOT here —
 * they are handled by the existing parseAndSaveToken / addToken error flows
 * (yellow text in UpdateToken, INVALID_TOKEN dialog in TokenManagement, Toast in Login).
 */
enum class TokenImportError {
    /** User pressed Back in the file picker — silent, no dialog. */
    CANCELLED,
    /** Selected file does not end with .txt */
    NOT_TXT_EXTENSION,
    /** SecurityException or FileNotFoundException when opening the stream. */
    FILE_NOT_READABLE,
    /** File content is empty after trimming. */
    FILE_EMPTY,
    /** IOException or other error while reading. */
    IO_ERROR,
}

/**
 * Remembers a launcher that opens the system file picker (filtered to .txt),
 * reads the selected file, and calls [onTokenLoaded] with the content.
 *
 * File-level errors go to [onError]. Token-content errors do NOT go here —
 * the loaded string is passed back to the caller, which fills the text field,
 * and the existing parseAndSaveToken / addToken flow handles token validation.
 *
 * TV-only affordance; callers gate on Context.isTv() (FEATURE_LEANBACK).
 */
@Composable
fun rememberTokenFilePicker(
    onTokenLoaded: (String) -> Unit,
    onError: (TokenImportError) -> Unit,
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            onError(TokenImportError.CANCELLED)
            return@rememberLauncherForActivityResult
        }

        // Validate .txt extension. uri.lastPathSegment is NOT always the real
        // file name: when a file is picked from "Recent" in the system picker,
        // some SAF providers (e.g. com.android.providers.downloads.documents)
        // return a URI whose lastPathSegment is a numeric document ID, which
        // made every Recent pick falsely fail the .txt check. Query
        // OpenableColumns.DISPLAY_NAME for the real name; fall back to the
        // decoded last path segment if the provider doesn't support the query.
        val fileName = run {
            var name: String? = null
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val n = cursor.getString(0)
                        if (!n.isNullOrEmpty()) name = n
                    }
                }
            } catch (_: Exception) {
                // best-effort — fall through to lastPathSegment below
            }
            name ?: uri.lastPathSegment?.let { Uri.decode(it) } ?: ""
        }
        if (!fileName.endsWith(".txt", ignoreCase = true)) {
            onError(TokenImportError.NOT_TXT_EXTENSION)
            return@rememberLauncherForActivityResult
        }

        // Read the entire file (no cap — token size is unknown)
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText().trim()
                }
            }
            if (content.isNullOrEmpty()) {
                onError(TokenImportError.FILE_EMPTY)
            } else {
                onTokenLoaded(content)
            }
        } catch (e: SecurityException) {
            onError(TokenImportError.FILE_NOT_READABLE)
        } catch (e: FileNotFoundException) {
            onError(TokenImportError.FILE_NOT_READABLE)
        } catch (e: IOException) {
            onError(TokenImportError.IO_ERROR)
        } catch (e: Exception) {
            onError(TokenImportError.IO_ERROR)
        }
    }

    // text/plain first so well-behaved pickers filter; */* fallback for TV file managers
    // that don't set MIME correctly. Some TVs have no file manager at all — OpenDocument
    // would crash with ActivityNotFoundException, so degrade to an import error instead
    // (same policy as IntentUtils.startIfResolvable).
    val mimeFilter = arrayOf("text/plain", "*/*")
    return remember(launcher) {
        {
            try {
                launcher.launch(mimeFilter)
            } catch (e: ActivityNotFoundException) {
                onError(TokenImportError.FILE_NOT_READABLE)
            }
        }
    }
}

/**
 * Error dialog for token file import errors.
 *
 * - Single line spacing (lineHeight = 17.sp).
 * - CANCELLED is silent (no dialog shown).
 *
 * Pass null or CANCELLED to hide the dialog.
 */
@Composable
fun TokenImportErrorDialog(
    error: TokenImportError?,
    onDismiss: () -> Unit,
) {
    if (error == null || error == TokenImportError.CANCELLED) {
        return
    }

    val messageResId = when (error) {
        TokenImportError.NOT_TXT_EXTENSION -> R.string.token_import_error_not_txt
        TokenImportError.FILE_NOT_READABLE -> R.string.token_import_error_read
        TokenImportError.FILE_EMPTY -> R.string.token_import_error_empty
        TokenImportError.IO_ERROR -> R.string.token_import_error_io
        TokenImportError.CANCELLED -> return // unreachable
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.token_error_title),
                color = White,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(messageResId),
                color = White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
