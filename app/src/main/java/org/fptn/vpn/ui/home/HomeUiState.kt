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
    val pingMs: Int = 42,
    val serverName: String = "Нидерланды Free",
    val serverRegion: String = "Амстердам",
    val serverFlag: String = "🇳🇱",
    val serverProtocol: String = "fptn",
    val downloadSpeed: String = "18.4 MB/s",
    val uploadSpeed: String = "4.2 MB/s",
    val downloadTraffic: String = "11.1 MiB",
    val uploadTraffic: String = "3.4 MiB",
    val noticeTitle: String = "Объявление",
    val noticeMessage: String = "Все серверы работают в штатном режиме. Защита активна.",
    val isNoticeVisible: Boolean = true,
    val services: List<ServicePing> = listOf(
        ServicePing("YouTube", "Доступен", true, 34),
        ServicePing("Telegram", "Доступен", true, 28),
        ServicePing("Discord", "Доступен", true, 45),
        ServicePing("OpenAI", "Доступен", true, 52),
    ),
)
