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

package org.fptn.vpn.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.database.entity.TokenEntity
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.token.TokenUtils
import org.fptn.vpn.exception.PVNClientException

class LoginActivityViewModel(application: Application) : AndroidViewModel(application) {

    val errorTextLiveData = MutableLiveData("")

    private val appDatabase = AppDatabase.getInstance(application)

    @Throws(PVNClientException::class)
    suspend fun parseAndSaveToken(token: String) {
        val parsed = TokenUtils.parseToken(token)
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val username = parsed.username
            val password = parsed.password
            val serviceName = parsed.serviceName ?: username

            var newTokenId = 0

            appDatabase.runInTransaction {
                val newToken = TokenEntity(
                    0,               // id
                    serviceName,     // serviceName
                    token,           // rawToken
                    username,        // username
                    password,        // password
                    true,            // isVisible
                    now,             // createdAt
                    now              // updatedAt
                )

                newTokenId = appDatabase.tokenDAO().insert(newToken).toInt()
                parsed.servers.forEach { it.tokenId = newTokenId }
                appDatabase.serverDAO().insertAll(parsed.servers)
            }

            SharedPrefUtils.saveTokenUpdatedDate(getApplication(), now)

            XLog.tag("LoginActivityViewModel").i("Token created [id=%d, serviceName=%s, servers=%d]",
                newTokenId, serviceName, parsed.servers.size)
        }
    }
}
