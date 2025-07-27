package org.fptn.vpn.auth.data.token

import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer

class AuthTokenNormalizerImpl : AuthTokenNormalizer {
    override fun normalize(token: String): String {
        val normalizedToken =
            token
                .replace("\\s+".toRegex(), "")
                .replace("fptn://", "")
                .replace("fptn:", "")
        val padding = (OFFSET - normalizedToken.length % OFFSET) % OFFSET
        val result = StringBuilder(normalizedToken)
        repeat(padding) { result.append("=") }
        return result.toString()
    }

    private companion object {
        const val OFFSET = 4
    }
}
