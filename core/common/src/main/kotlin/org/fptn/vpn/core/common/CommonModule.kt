package org.fptn.vpn.core.common

import kotlinx.coroutines.Dispatchers
import org.fptn.vpn.core.common.AgDispatchers.DISPATCHER_DEFAULT
import org.fptn.vpn.core.common.AgDispatchers.DISPATCHER_IO
import org.fptn.vpn.core.common.AgDispatchers.DISPATCHER_UNCONFINED
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coroutineModule =
    module {
        single(named(DISPATCHER_DEFAULT)) { Dispatchers.Default }
        single(named(DISPATCHER_IO)) { Dispatchers.IO }
        single(named(DISPATCHER_UNCONFINED)) { Dispatchers.Unconfined }
    }

val commonModule =
    module {
        includes(coroutineModule)
    }
