package org.fptn.vpn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FptnUser(
    @SerialName("version") val version: Int,
    @SerialName("service_name") val serviceName: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("servers") val servers: List<FptnServer> = emptyList(),
    @SerialName("censored_zone_servers") val censoredZoneServers: List<FptnServer> = emptyList(),
)

@Serializable
data class FptnServer(
    @SerialName("name") val name: String,
    @SerialName("host") val host: String,
    @SerialName("md5_fingerprint") val md5Fingerprint: String,
    @SerialName("port") val port: Int,
)

fun FptnUser.toDomain() =
    FptnUserDomain(
        version = version,
        serviceName = serviceName,
        username = username,
        password = password,
        servers = servers.map { it.toDomain() },
        censoredZoneServers = censoredZoneServers.map { it.toDomain() },
    )

fun FptnServer.toDomain() =
    FptnServerDomain(
        name = name,
        host = host,
        md5Fingerprint = md5Fingerprint,
        port = port,
    )
