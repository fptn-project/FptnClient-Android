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
import org.fptn.vpn.vpnclient.exception.PVNClientException

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