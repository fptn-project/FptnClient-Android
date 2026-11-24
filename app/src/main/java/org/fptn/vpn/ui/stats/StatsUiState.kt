package org.fptn.vpn.ui.stats

data class DailyTraffic(
    val dayLabel: String,
    val volumeGiB: Float,
    val isMax: Boolean = false,
)

data class StatsUiState(
    val weeklyTraffic: List<DailyTraffic> = listOf(
        DailyTraffic("Пн", 0f),
        DailyTraffic("Вт", 0f),
        DailyTraffic("Ср", 0f),
        DailyTraffic("Чт", 0f),
        DailyTraffic("Пт", 0f),
        DailyTraffic("Сб", 0f),
        DailyTraffic("Вс", 0f),
    ),
    val totalWeeklyTraffic: String = "0 Б",
    val currentDownloadSpeed: String = "0 Б/с",
    val currentUploadSpeed: String = "0 Б/с",
    val peakSpeed: String = "0 Б/с",
    val sessionDuration: String = "00:00:00",
    val totalDownloaded: String = "0 Б",
    val totalUploaded: String = "0 Б",
    val packetLossPercent: Float = 0.0f,
    val memoryUsageMb: String = "0 МБ",
    val activeThreads: Int = 1,
    val lastResetTime: String = "—",
)
