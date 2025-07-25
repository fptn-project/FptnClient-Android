package org.fptn.vpn.auth.domain.token

interface AuthTokenNormalizer {
    fun normalize(token: String): String
}
