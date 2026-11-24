package org.fptn.vpn.ui.home

import org.fptn.vpn.enums.ConnectionState

data class ServicePing(
    val name: String,
    val statusText: String,
    val isAvailable: Boolean,
    val pingMs: Int,
)

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val greeting: String = "Добрый день",
    val sessionTimerText: String = "00:00:00",
    val pingMs: Int = -1,
    val serverName: String = "Авто-выбор",
    val serverRegion: String = "Оптимальный маршрут",
    val serverFlag: String = "⚡",
    val serverProtocol: String = "fptn",
    val downloadSpeed: String = "0 Б/с",
    val uploadSpeed: String = "0 Б/с",
    val downloadTraffic: String = "0 Б",
    val uploadTraffic: String = "0 Б",
    val noticeTitle: String = "Объявление",
    val noticeMessage: String = "Все серверы работают в штатном режиме. Защита активна.",
    val isNoticeVisible: Boolean = true,
    val services: List<ServicePing> = listOf(
        ServicePing("YouTube", "Ожидание", false, -1),
        ServicePing("Telegram", "Ожидание", false, -1),
        ServicePing("Discord", "Ожидание", false, -1),
        ServicePing("OpenAI", "Ожидание", false, -1),
    ),
)
