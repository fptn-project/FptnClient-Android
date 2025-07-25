package org.fptn.vpn.auth.data.token

import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthTokenNormalizerTest {
    private val normalizer: AuthTokenNormalizer = AuthTokenNormalizerImpl()

    @Test
    fun `normalize should remove whitespaces`() {
        val input = "  token with spaces  "
        val expected = "tokenwithspaces"
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `normalize should remove fptn protocol prefix`() {
        val input = "fptn://mysome-token"
        val expected = "mysome-token"
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `normalize should remove fptn prefix`() {
        val input = "fptn:mysome-token"
        val expected = "mysome-token"
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `normalize should handle combined cases`() {
        val input = "  fptn:// token with spaces  "
        val expected = "tokenwithspaces"
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `normalize should return empty string for empty input`() {
        val input = ""
        val expected = ""
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `token has no padding`() {
        val input = "token1"
        val expected = "token1"
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `token has one padding`() {
        val input = "token12"
        val expected = "token12=="
        assertEquals(expected, normalizer.normalize(input))
    }

    @Test
    fun `token has two paddings`() {
        val input = "token123"
        val expected = "token123="
        assertEquals(expected, normalizer.normalize(input))
    }
}
