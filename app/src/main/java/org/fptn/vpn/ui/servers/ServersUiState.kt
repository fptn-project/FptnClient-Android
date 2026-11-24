package org.fptn.vpn.ui.servers

data class ServerProfile(
    val id: String,
    val title: String,
    val emoji: String = "🔥",
    val updatedAt: String,
    val trafficUsed: String,
    val trafficLimit: String,
    val trafficProgress: Float,
    val daysLeftText: String,
    val isSelected: Boolean = false,
)

data class ServerItem(
    val id: String,
    val name: String,
    val region: String,
    val flag: String,
    val pingMs: Int,
    val protocol: String = "fptn",
    val port: Int = 443,
    val isAutoSelect: Boolean = false,
    val isSelected: Boolean = false,
    val isOnline: Boolean = true,
)

enum class ServerSortFilter(val label: String) {
    DEFAULT("по Конфигу"),
    FASTEST("по Пингу"),
    AVAILABLE("Доступные"),
}

data class ServersUiState(
    val profiles: List<ServerProfile> = emptyList(),
    val isProfilesExpanded: Boolean = false,
    val isRefreshingProfile: Boolean = false,
    val servers: List<ServerItem> = emptyList(),
    val sortFilter: ServerSortFilter = ServerSortFilter.DEFAULT,
    val isCheckingPing: Boolean = false,
    val showAddTokenSheet: Boolean = false,
    val tokenInputText: String = "",
    val tokenInputError: String? = null,
) {
    val selectedProfile: ServerProfile?
        get() = profiles.firstOrNull { it.isSelected } ?: profiles.firstOrNull()

    val selectedServer: ServerItem?
        get() = servers.firstOrNull { it.isSelected } ?: servers.firstOrNull()

    val displayServers: List<ServerItem>
        get() {
            val (autoNodes, regularNodes) = servers.partition { it.isAutoSelect }
            val sortedRegular = when (sortFilter) {
                ServerSortFilter.DEFAULT -> regularNodes
                ServerSortFilter.FASTEST -> regularNodes.sortedBy { it.pingMs }
                ServerSortFilter.AVAILABLE -> regularNodes.filter { it.isOnline }
            }
            return autoNodes + sortedRegular
        }
}
