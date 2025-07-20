package org.fptn.vpn.di

import com.filantrop.pvnclient.auth.ui.authModule
import org.fptn.vpn.core.common.commonModule
import org.fptn.vpn.core.network.networkModule
import org.fptn.vpn.core.persistent.di.persistentModule
import org.fptn.vpn.viewmodel.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { MainViewModel(get()) }
    }

val appModule: Module =
    module {
        includes(
            authModule,
            commonModule,
            persistentModule,
            networkModule,
            viewModelModule,
        )
    }
