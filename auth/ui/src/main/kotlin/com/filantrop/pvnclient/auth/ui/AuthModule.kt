package com.filantrop.pvnclient.auth.ui

import org.fptn.vpn.auth.data.di.authDataModule
import org.fptn.vpn.auth.data.di.authTokenModule
import org.fptn.vpn.auth.domain.di.authDomainModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { AuthViewModel(get()) }
    }

val authModule =
    module {
        includes(
            authTokenModule,
            viewModelModule,
            authDataModule,
            authDomainModule,
        )
    }
