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
import org.fptn.vpn.vpnclient.exception.PVNClientException

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