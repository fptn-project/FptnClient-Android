package org.fptn.vpn.auth.data.di

import org.fptn.vpn.auth.data.AuthRepositoryImpl
import org.fptn.vpn.auth.data.token.AuthTokenDecoderImpl
import org.fptn.vpn.auth.data.token.AuthTokenNormalizerImpl
import org.fptn.vpn.auth.domain.AuthRepository
import org.fptn.vpn.auth.domain.token.AuthTokenDecoder
import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer
import org.fptn.vpn.core.common.AppDispatchers.DISPATCHER_IO
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authDataModule =
    module {
        single<AuthRepository> {
            AuthRepositoryImpl(
                get(),
                get(),
                get(),
                get(named(DISPATCHER_IO)),
            )
        }
    }

val authTokenModule =
    module {
        single<AuthTokenNormalizer> { AuthTokenNormalizerImpl() }
        single<AuthTokenDecoder> { AuthTokenDecoderImpl(get()) }
    }
