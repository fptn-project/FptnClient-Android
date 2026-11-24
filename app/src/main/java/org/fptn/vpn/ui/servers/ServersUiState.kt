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
    val profiles: List<ServerProfile> = listOf(
        ServerProfile(
            id = "profile_1",
            title = "FPTN Prime Unlimited",
            emoji = "🔥",
            updatedAt = "Сегодня в 17:25",
            trafficUsed = "11,1 MiB",
            trafficLimit = "35,0 GiB",
            trafficProgress = 0.32f,
            daysLeftText = "Осталось 28 дней",
            isSelected = true,
        ),
        ServerProfile(
            id = "profile_2",
            title = "Quattro Community Node",
            emoji = "⚡",
            updatedAt = "Вчера в 05:06",
            trafficUsed = "12,4 GiB",
            trafficLimit = "1,6 TiB",
            trafficProgress = 0.08f,
            daysLeftText = "Осталось 26/30 дней",
            isSelected = false,
        ),
        ServerProfile(
            id = "profile_3",
            title = "Backup Relay Pool",
            emoji = "🛡",
            updatedAt = "23 ноября в 01:47",
            trafficUsed = "1,2 GiB",
            trafficLimit = "50,0 GiB",
            trafficProgress = 0.02f,
            daysLeftText = "Осталось 90 дней",
            isSelected = false,
        ),
    ),
    val isProfilesExpanded: Boolean = false,
    val isRefreshingProfile: Boolean = false,
    val servers: List<ServerItem> = listOf(
        ServerItem(
            id = "srv_auto",
            name = "Авто-выбор (Самый быстрый)",
            region = "Автоматический маршрут",
            flag = "⚡",
            pingMs = 35,
            protocol = "fptn",
            port = 443,
            isAutoSelect = true,
            isSelected = false,
        ),
        ServerItem("srv_1", "Нидерланды Free", "Амстердам", "🇳🇱", 42, "fptn", 443, isSelected = true),
        ServerItem("srv_2", "Германия Fast", "Франкфурт", "🇩🇪", 58, "fptn", 443, isSelected = false),
        ServerItem("srv_3", "Польша #1", "Варшава", "🇵🇱", 74, "fptn", 443, isSelected = false),
        ServerItem("srv_4", "Финляндия Shield", "Хельсинки", "🇫🇮", 89, "fptn", 443, isSelected = false),
        ServerItem("srv_5", "Санкт-Петербург", "Северо-Запад", "🇷🇺", 35, "fptn", 443, isSelected = false),
        ServerItem("srv_6", "Новосибирск", "Сибирь", "🇷🇺", 134, "fptn", 443, isSelected = false),
    ),
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
