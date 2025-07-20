package org.fptn.vpn.core.persistent.di

import org.fptn.vpn.core.persistent.PreferenceStore
import org.fptn.vpn.core.persistent.PreferencesStoreImpl
import org.koin.dsl.module

val persistentModule =
    module {
        single<PreferenceStore> { PreferencesStoreImpl(get()) }
    }
