package org.fptn.vpn.auth.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.fptn.vpn.auth.domain.AuthRepository
import org.fptn.vpn.auth.domain.token.AuthTokenDecoder
import org.fptn.vpn.core.common.AppDispatchers.DISPATCHER_IO
import org.fptn.vpn.core.model.FptnUserDomain
import org.fptn.vpn.core.persistent.PreferenceStore
import org.fptn.vpn.core.persistent.model.FptnServerDao
import org.fptn.vpn.core.persistent.model.FptnServerDbModel
import org.fptn.vpn.core.persistent.model.toDbModel
import org.koin.core.annotation.Named

class AuthRepositoryImpl(
    private val serverDao: FptnServerDao,
    private val preferenceStore: PreferenceStore,
    private val tokenDecoder: AuthTokenDecoder,
    @Named(DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
) : AuthRepository {
    override val user: Flow<FptnUserDomain> = preferenceStore.token.map { tokenDecoder.decode(it) }

    override suspend fun saveToken(token: String) {
        withContext(dispatcher) {
            preferenceStore.updateToken(token)
            val model: FptnUserDomain = tokenDecoder.decode(token)
            val username = model.username
            val password = model.password
            val servers: List<FptnServerDbModel> = model.servers.map { it.toDbModel(username, password, false) }
            val censoredZoneServers: List<FptnServerDbModel> =
                model.censoredZoneServers.map { it.toDbModel(username, password, true) }
            val allServers = servers + censoredZoneServers
            allServers.map { async { serverDao.insert(it) } }.awaitAll()
        }
    }

    override suspend fun logout() = preferenceStore.clearAllData()
}
