package org.fptn.vpn.auth.data.token

import io.mockk.every
import io.mockk.mockk
import org.fptn.vpn.auth.domain.token.AuthTokenDecoder
import org.fptn.vpn.auth.domain.token.AuthTokenNormalizer
import org.fptn.vpn.core.model.FptnServerDomain
import org.fptn.vpn.core.model.FptnUserDomain
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthTokenDecoderTest {
    private val normalizer = mockk<AuthTokenNormalizer>()
    private val decoder: AuthTokenDecoder by lazy { AuthTokenDecoderImpl(normalizer) }

    @Test
    fun `decode successful`() {
        val token =
            "eyJ2ZXJzaW9uIjogMSwgInNlcnZpY2VfbmFtZSI6ICJGUFROLk9OTElORSIsICJ1c2VybmFtZSI6ICJteV9hd2Vz" +
                "b21lX3VzZXJfbmFtZSIsICJwYXNzd29yZCI6ICJteV9hd2Vzb21lX3Bhc3N3b3JkIiwgInNlcnZlcnMiOiBbeyJ" +
                "uYW1lIjogIkVzdG9uaWEiLCAiaG9zdCI6ICIxOTIuMTY4LjEuMiIsICJtZDVfZmluZ2VycHJpbnQiOiAiZDAwOWZ" +
                "kOWNlYjI4MzEyMzgzMmU1YWQzZWNhMDIzNGEiLCAicG9ydCI6IDQ0M30sIHsibmFtZSI6ICJMYXR2aWEtMSIsICJ" +
                "ob3N0IjogIjE5Mi4xNjguMS4zIiwgIm1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzODMyZTVhZDN" +
                "lY2EwMjM0YiIsICJwb3J0IjogNDQzfSwgeyJuYW1lIjogIkxhdHZpYS0yIiwgImhvc3QiOiAiMTkyLjE2OC4xLjQiL" +
                "CAibWQ1X2ZpbmdlcnByaW50IjogImQwMDlmZDljZWIyODMxMjM4MzJlNWFkM2VjYTAyMzRj" +
                "IiwgInBvcnQiOiA0NDN9LCB7Im5hbWUiOiAiTmV0aGVybGFuZHMtMSIsICJob3N0IjogIjE5Mi4xNjguMS41IiwgIm" +
                "1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzODMyZTVhZDNlY2EwMjM0ZCIsICJwb3J0IjogNDQzfS" +
                "wgeyJuYW1lIjogIlVTQS1TZWF0dGxlIiwgImhvc3QiOiAiMTkyLjE2OC4xLjYiLCAibWQ1X2ZpbmdlcnByaW50Ij" +
                "ogImQwMDlmZDljZWIyODMxMjM4MzJlNWFkM2VjYTAyMzJhIiwgInBvcnQiOiA0NDN9LCB7Im5hbWUiOiAiSmFwYW" +
                "4tMSIsICJob3N0IjogIjE5Mi4xNjguMS43IiwgIm1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzODMy" +
                "ZTVhZDNlY2EwMjMyYiIsICJwb3J0IjogNDQzfV0sICJjZW5zb3JlZF96b25lX3NlcnZlcnMiOiBbeyJuYW1lIjogI" +
                "lJ1c3NpYSAoU2FpbnQgUGV0ZXJzYnVyZykiLCAiaG9zdCI6ICIxOTIuMTY4LjEuOCIsICJtZDVfZmluZ2VycHJpbn" +
                "QiOiAiZDAwOWZkOWNlYjI4MzEyMzgzMmU1YWQzZWNhMDIzMmMiLCAicG9ydCI6IDQ0M31dfQ=="
        every { normalizer.normalize(token) } returns token

        val res = decoder.decode(token)

        val expected =
            FptnUserDomain(
                version = 1,
                serviceName = "FPTN.ONLINE",
                username = "my_awesome_user_name",
                password = "my_awesome_password",
                servers =
                    listOf(
                        FptnServerDomain(
                            name = "Estonia",
                            host = "192.168.1.2",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234a",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Latvia-1",
                            host = "192.168.1.3",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234b",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Latvia-2",
                            host = "192.168.1.4",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234c",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Netherlands-1",
                            host = "192.168.1.5",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234d",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "USA-Seattle",
                            host = "192.168.1.6",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232a",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Japan-1",
                            host = "192.168.1.7",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232b",
                            port = 443,
                        ),
                    ),
                censoredZoneServers =
                    listOf(
                        FptnServerDomain(
                            name = "Russia (Saint Petersburg)",
                            host = "192.168.1.8",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232c",
                            port = 443,
                        ),
                    ),
            )
        assertEquals(expected, res)
    }

    @Test
    fun `decode with both empty servers`() {
        val token =
            "eyJ2ZXJzaW9uIjogMSwgInNlcnZpY2VfbmFtZSI6ICJGUFROLk9OTElORSIsICJ1c2VybmFtZSI6ICJteV9hd" +
                "2Vzb21lX3VzZXJfbmFtZSIsICJwYXNzd29yZCI6ICJteV9hd2Vzb21lX3Bhc3N3b3JkIiwgInNlcnZlcnMiO" +
                "iBbXSwgImNlbnNvcmVkX3pvbmVfc2VydmVycyI6IFtdfQ=="
        every { normalizer.normalize(token) } returns token

        val res = decoder.decode(token)

        val expected =
            FptnUserDomain(
                version = 1,
                serviceName = "FPTN.ONLINE",
                username = "my_awesome_user_name",
                password = "my_awesome_password",
            )
        assertEquals(expected, res)
    }

    @Test
    fun `decode with empty servers`() {
        val token =
            "eyJ2ZXJzaW9uIjogMSwgInNlcnZpY2VfbmFtZSI6ICJGUFROLk9OTElORSIsICJ1c2VybmFtZSI6ICJteV9hd2Vzb" +
                "21lX3VzZXJfbmFtZSIsICJwYXNzd29yZCI6ICJteV9hd2Vzb21lX3Bhc3N3b3JkIiwgInNlcnZlcnMiOiBb" +
                "XSwgImNlbnNvcmVkX3pvbmVfc2VydmVycyI6IFt7Im5hbWUiOiAiUnVzc2lhIChTYWludCBQZXRlcnNidXJnKS" +
                "IsICJob3N0IjogIjE5Mi4xNjguMS44IiwgIm1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzOD" +
                "MyZTVhZDNlY2EwMjMyYyIsICJwb3J0IjogNDQzfV19"
        every { normalizer.normalize(token) } returns token

        val res = decoder.decode(token)

        val expected =
            FptnUserDomain(
                version = 1,
                serviceName = "FPTN.ONLINE",
                username = "my_awesome_user_name",
                password = "my_awesome_password",
                censoredZoneServers =
                    listOf(
                        FptnServerDomain(
                            name = "Russia (Saint Petersburg)",
                            host = "192.168.1.8",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232c",
                            port = 443,
                        ),
                    ),
            )
        assertEquals(expected, res)
    }

    @Test
    fun `decode with empty censored servers`() {
        val token =
            "eyJ2ZXJzaW9uIjogMSwgInNlcnZpY2VfbmFtZSI6ICJGUFROLk9OTElORSIsICJ1c2VybmFtZSI6ICJteV9hd2Vzb2" +
                "1lX3VzZXJfbmFtZSIsICJwYXNzd29yZCI6ICJteV9hd2Vzb21lX3Bhc3N3b3JkIiwgInNlcnZlcnMiOiBbeyJuYW" +
                "1lIjogIkVzdG9uaWEiLCAiaG9zdCI6ICIxOTIuMTY4LjEuMiIsICJtZDVfZmluZ2VycHJpbnQiOiAiZDAwOWZkOWN" +
                "lYjI4MzEyMzgzMmU1YWQzZWNhMDIzNGEiLCAicG9ydCI6IDQ0M30sIHsibmFtZSI6ICJMYXR2aWEtMSIsICJob3N0I" +
                "jogIjE5Mi4xNjguMS4zIiwgIm1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzODMyZTVhZDNlY2EwMjM" +
                "0YiIsICJwb3J0IjogNDQzfSwgeyJuYW1lIjogIkxhdHZpYS0yIiwgImhvc3QiOiAiMTkyLjE2OC4xLjQiLCAibWQ1X2" +
                "ZpbmdlcnByaW50IjogImQwMDlmZDljZWIyODMxMjM4MzJlNWFkM2VjYTAyMzRjIiwgInBvcnQiOiA0NDN9LCB7Im5h" +
                "bWUiOiAiTmV0aGVybGFuZHMtMSIsICJob3N0IjogIjE5Mi4xNjguMS41IiwgIm1kNV9maW5nZXJwcmludCI6ICJkMD" +
                "A5ZmQ5Y2ViMjgzMTIzODMyZTVhZDNlY2EwMjM0ZCIsICJwb3J0IjogNDQzfSwgeyJuYW1lIjogIlVTQS1TZWF0dGxl" +
                "IiwgImhvc3QiOiAiMTkyLjE2OC4xLjYiLCAibWQ1X2ZpbmdlcnByaW50IjogImQwMDlmZDljZWIyODMxMjM4MzJlNW" +
                "FkM2VjYTAyMzJhIiwgInBvcnQiOiA0NDN9LCB7Im5hbWUiOiAiSmFwYW4tMSIsICJob3N0IjogIjE5Mi4xNjguMS43" +
                "IiwgIm1kNV9maW5nZXJwcmludCI6ICJkMDA5ZmQ5Y2ViMjgzMTIzODMyZTVhZDNlY2EwMjMyYiIsICJwb3J0IjogN" +
                "DQzfV0sICJjZW5zb3JlZF96b25lX3NlcnZlcnMiOiBbXX0="
        every { normalizer.normalize(token) } returns token

        val res = decoder.decode(token)

        val expected =
            FptnUserDomain(
                version = 1,
                serviceName = "FPTN.ONLINE",
                username = "my_awesome_user_name",
                password = "my_awesome_password",
                servers =
                    listOf(
                        FptnServerDomain(
                            name = "Estonia",
                            host = "192.168.1.2",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234a",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Latvia-1",
                            host = "192.168.1.3",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234b",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Latvia-2",
                            host = "192.168.1.4",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234c",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Netherlands-1",
                            host = "192.168.1.5",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0234d",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "USA-Seattle",
                            host = "192.168.1.6",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232a",
                            port = 443,
                        ),
                        FptnServerDomain(
                            name = "Japan-1",
                            host = "192.168.1.7",
                            md5Fingerprint = "d009fd9ceb283123832e5ad3eca0232b",
                            port = 443,
                        ),
                    ),
            )
        assertEquals(expected, res)
    }

    @Test
    fun `decode with exception empty servers`() {
        val invalidToken =
            "eyJ2ZXJzaW9uIjogMSwgInNlcnZpY2VfbmFtZSI6ICJGUFROLk9OTElORSIsICJ1c2VybmFtZSI6ICJteV9hd2Vzb21lX" +
                "3VzZXJfbmFtZSIsICJwYXNzd29yZCI6ICJteV9hd2Vzb21lX3Bhc3N3b3JkIiwgInNlcnZlcnMiOiBbXSwgImNlbn" +
                "NvcmVkX3pvbmVfc2VydmVycyI6IFtdfQ"
        every { normalizer.normalize(invalidToken) } returns invalidToken

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                decoder.decode(invalidToken)
            }

        assertTrue(exception.message?.isNotEmpty() == true)
    }
}
