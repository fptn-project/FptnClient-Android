package org.fptn.vpn.auth.data.token

import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer

class AuthTokenNormalizerImpl : AuthTokenNormalizer {
    override fun normalize(token: String): String {
        val normalizedToken =
            token
                .replace("\\s+".toRegex(), "")
                .replace("fptn://", "")
                .replace("fptn:", "")
        return when (normalizedToken.length % (OFFSET - 1)) {
            1 -> "$normalizedToken=="
            2 -> "$normalizedToken="
            else -> normalizedToken
        }
    }

    private companion object {
        const val OFFSET = 4
    }
}
