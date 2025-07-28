package org.fptn.vpn.vpnclient.exception

enum class ErrorCode(
    val value: String,
) {
    SERVER_LIST_NULL_OR_EMPTY("server_list_is_empty"),
    FIND_FASTEST_SERVER_TIMEOUT("find_fastest_server_timeout"),
    ALL_SERVERS_UNREACHABLE("all_servers_are_unreachable"),
    NO_ACTIVE_INTERNET_CONNECTIONS("no_active_internet_connections"),
    CONNECT_TO_SERVER_ERROR("connect_to_server_error"),
    RECONNECTING_FAILED("reconnecting_failed"),
    DNS_SERVER_ERROR("dns_server_error"),
    VPN_INTERFACE_ERROR("vpn_interface_error"),
    CIPHERS_ERROR("ciphers_error"),
    SSL_CONTEXT_INIT_FAILED("ssl_context_init_failed"),
    ACCESS_TOKEN_ERROR("access_token_error"),
    ACCESS_TOKEN_FORMAT_ERROR("access_token_format"),
    UNKNOWN_ERROR("unknown_error"),
    ;

    companion object {
        private val errorCodesWithOfferingRefreshToken =
            listOf(
                SERVER_LIST_NULL_OR_EMPTY,
                FIND_FASTEST_SERVER_TIMEOUT,
                ALL_SERVERS_UNREACHABLE,
                ACCESS_TOKEN_ERROR,
            )

        fun isNeedToOfferRefreshToken(errorCode: ErrorCode): Boolean {
            return errorCodesWithOfferingRefreshToken.contains(errorCode)
        }

        fun getErrorCodeByValue(value: String): ErrorCode {
            return entries
                .stream()
                .filter { it.value == value }
                .findAny()
                .orElse(UNKNOWN_ERROR)
        }
    }
}
