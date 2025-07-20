package org.fptn.vpn.core.network

import org.koin.dsl.module

val networkModule =
    module {
        single<NetworkMonitor> { ConnectivityManagerNetworkMonitor(get(), get()) }
    }
