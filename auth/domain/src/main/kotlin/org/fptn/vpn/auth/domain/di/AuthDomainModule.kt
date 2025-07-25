package org.fptn.vpn.auth.domain.di

import org.fptn.vpn.auth.domain.AuthInteractor
import org.fptn.vpn.auth.domain.AuthInteractorImpl
import org.koin.dsl.module

val authDomainModule =
    module {
        single<AuthInteractor> { AuthInteractorImpl(get()) }
    }
