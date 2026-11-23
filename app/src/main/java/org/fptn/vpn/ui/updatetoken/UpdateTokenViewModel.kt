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

package org.fptn.vpn.ui.updatetoken

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.token.TokenUtils
import org.fptn.vpn.exception.PVNClientException

class UpdateTokenViewModel(application: Application) : AndroidViewModel(application) {

    val errorTextLiveData = MutableLiveData("")

    private val appDatabase = AppDatabase.getInstance(application)

    @Throws(PVNClientException::class)
    suspend fun parseAndSaveToken(token: String): Boolean {
        val parsed = TokenUtils.parseToken(token)
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val username = parsed.username
            val password = parsed.password
            val serviceName = parsed.serviceName ?: username

            val existingToken = appDatabase.tokenDAO().findByUsername(username)

            if (existingToken != null) {
                appDatabase.runInTransaction {
                    appDatabase.tokenDAO().updateTokenData(
                        existingToken.id, serviceName, token, username, password, now
                    )
                    appDatabase.serverDAO().deleteByTokenId(existingToken.id)
                    parsed.servers.forEach { it.tokenId = existingToken.id }
                    appDatabase.serverDAO().insertAll(parsed.servers)
                }

                SharedPrefUtils.saveTokenUpdatedDate(getApplication(), now)
                XLog.tag("UpdateTokenViewModel").i("Token updated [id=%d, serviceName=%s, servers=%d]",
                    existingToken.id, serviceName, parsed.servers.size)
                true
            } else {
                false
            }
        }
    }
}
