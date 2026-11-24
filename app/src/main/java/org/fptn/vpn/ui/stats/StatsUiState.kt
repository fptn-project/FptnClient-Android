package org.fptn.vpn.ui.stats

data class DailyTraffic(
    val dayLabel: String,
    val volumeGiB: Float,
    val isMax: Boolean = false,
)

data class StatsUiState(
    val weeklyTraffic: List<DailyTraffic> = listOf(
        DailyTraffic("С", 2.4f),
        DailyTraffic("Ч", 4.8f),
        DailyTraffic("П", 5.6f, isMax = true),
        DailyTraffic("С", 2.2f),
        DailyTraffic("В", 3.7f),
        DailyTraffic("П", 5.2f),
        DailyTraffic("В", 0.6f),
    ),
    val totalWeeklyTraffic: String = "27.4 GB",
    val currentDownloadSpeed: String = "18.4 MB/s",
    val currentUploadSpeed: String = "4.2 MB/s",
    val peakSpeed: String = "28.5 MB/s",
    val sessionDuration: String = "00:14:32",
    val totalDownloaded: String = "19.8 GB",
    val totalUploaded: String = "7.6 GB",
    val packetLossPercent: Float = 0.0f,
    val memoryUsageMb: String = "28.4 MB",
    val activeThreads: Int = 12,
    val lastResetTime: String = "07:23",
)
