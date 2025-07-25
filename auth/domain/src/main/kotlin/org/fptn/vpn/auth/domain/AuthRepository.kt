package org.fptn.vpn.auth.domain

import kotlinx.coroutines.flow.Flow
import org.fptn.vpn.core.model.FptnUserDomain

interface AuthRepository {
    val user: Flow<FptnUserDomain>

    suspend fun saveToken(token: String)

    suspend fun logout()
}
