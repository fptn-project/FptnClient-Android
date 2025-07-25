package org.fptn.vpn.auth.data.token

import kotlinx.serialization.json.Json
import org.fptn.vpn.auth.domain.token.AuthTokenDecoder
import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer
import org.fptn.vpn.core.model.FptnUser
import org.fptn.vpn.core.model.FptnUserDomain
import org.fptn.vpn.core.model.toDomain
import kotlin.io.encoding.Base64

class AuthTokenDecoderImpl(
    val tokenNormalizer: AuthTokenNormalizer,
) : AuthTokenDecoder {
    override fun decode(token: String): FptnUserDomain {
        val normalizedToken = tokenNormalizer.normalize(token)
        val decodedBytes = String(Base64.decode(normalizedToken), Charsets.UTF_8)
        return Json.decodeFromString<FptnUser>(decodedBytes).toDomain()
    }
}
