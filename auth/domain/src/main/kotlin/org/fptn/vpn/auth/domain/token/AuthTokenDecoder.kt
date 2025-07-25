package org.fptn.vpn.auth.domain.token

import org.fptn.vpn.core.model.FptnUserDomain

interface AuthTokenDecoder {
    fun decode(token: String): FptnUserDomain
}
