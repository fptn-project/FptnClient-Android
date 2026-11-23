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

package org.fptn.vpn.ui.tokens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.database.entity.TokenEntity
import org.fptn.vpn.utils.token.TokenUtils
import org.fptn.vpn.exception.PVNClientException

class TokenManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    val tokensLiveData = MutableLiveData<List<TokenEntity>>()

    init {
        loadTokens()
    }

    fun loadTokens() {
        viewModelScope.launch(Dispatchers.IO) {
            val tokens = appDatabase.tokenDAO().getAll()
            tokensLiveData.postValue(tokens)
        }
    }

    fun setTokenVisible(token: TokenEntity, isVisible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // If attempting to hide a token, check whether other visible tokens exist.
            // If no more visible tokens remain, prevent hiding it (to avoid an empty server list).
            if (!isVisible) {
                val visibleTokens = appDatabase.tokenDAO().getAll().filter { it.isVisible }
                if (visibleTokens.size <= 1) {
                    loadTokens() // Reload so that the Switch returns to ON
                    return@launch
                }
            }
            appDatabase.tokenDAO().setVisible(token.id, isVisible)
            loadTokens()
        }
    }

    fun deleteToken(token: TokenEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.runInTransaction {
                val tokens = appDatabase.tokenDAO().getAll()
                val visibleTokens = tokens.filter { it.isVisible }
                val canDelete = tokens.size > 1 &&
                  (token.isVisible && visibleTokens.size > 1 || !token.isVisible)

                if (canDelete) {
                    appDatabase.serverDAO().deleteByTokenId(token.id)
                    appDatabase.tokenDAO().deleteById(token.id)
                }
            }
            loadTokens()
        }
    }

    fun addToken(rawToken: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = TokenUtils.parseToken(rawToken)
                val username = parsed.username

                val existingToken = appDatabase.tokenDAO().findByUsername(username)
                if (existingToken != null) {
                    callback(false, "TOKEN_EXISTS")
                    return@launch
                }

                val now = System.currentTimeMillis()
                val serviceName = parsed.serviceName ?: username
                val password = parsed.password

                // Create the token, but do NOT make it visible (isVisible = false)
                val newToken = TokenEntity(
                    0, serviceName, rawToken, username, password, false, now, now
                )
                val newId = appDatabase.tokenDAO().insert(newToken).toInt()
                parsed.servers.forEach { it.tokenId = newId }
                appDatabase.serverDAO().insertAll(parsed.servers)

                callback(true, null)
                loadTokens()
            } catch (e: PVNClientException) {
                callback(false, "INVALID_TOKEN")
            } catch (e: Exception) {
                // Any other error (e.g., Base64) is also treated as an invalid token.
                callback(false, "INVALID_TOKEN")
            }
        }
    }
}
