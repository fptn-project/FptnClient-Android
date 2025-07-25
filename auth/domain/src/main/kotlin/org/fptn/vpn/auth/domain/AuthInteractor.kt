package org.fptn.vpn.auth.domain

import kotlinx.coroutines.flow.Flow
import org.fptn.vpn.core.model.FptnUserDomain

interface AuthInteractor {
    val user: Flow<FptnUserDomain>

    suspend fun saveToken(token: String)

    suspend fun logout()
}

class AuthInteractorImpl(
    private val authRepository: AuthRepository,
) : AuthInteractor {
    override val user: Flow<FptnUserDomain> = authRepository.user

    override suspend fun saveToken(token: String) = authRepository.saveToken(token)

    override suspend fun logout() = authRepository.logout()
}
