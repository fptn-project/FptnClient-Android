package org.fptn.vpn.core.model

data class FptnUserDomain(
    val version: Int,
    val serviceName: String,
    val username: String,
    val password: String,
    val servers: List<FptnServerDomain> = emptyList(),
    val censoredZoneServers: List<FptnServerDomain> = emptyList(),
)

data class FptnServerDomain(
    val name: String,
    val host: String,
    val md5Fingerprint: String,
    val port: Int,
)
