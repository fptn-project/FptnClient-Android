package org.fptn.vpn.ui.stats

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fptn.vpn.services.vpn.FptnService
import org.fptn.vpn.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "StatsViewModel"

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var subscribedService: FptnService? = null
    private var serviceConnection: ServiceConnection? = null

    private var speedObserver: Observer<Triple<String, String, Long>>? = null
    private var trafficObserver: Observer<Pair<Long, Long>>? = null

    private var maxRecordedSpeedBytesPerSec: Long = 0L

    init {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        _uiState.update { it.copy(lastResetTime = sdf.format(Date())) }

        bindFptnService()

        // Periodic telemetry monitor for runtime memory and active threads
        viewModelScope.launch {
            while (true) {
                delay(2000)
                updateProcessDiagnostics()
            }
        }
    }

    private fun updateProcessDiagnostics() {
        try {
            val runtime = Runtime.getRuntime()
            val usedBytes = runtime.totalMemory() - runtime.freeMemory()
            val usedMb = String.format(Locale("ru", "RU"), "%.1f МБ", usedBytes / (1024.0 * 1024.0))
            val threadCount = Thread.activeCount()

            _uiState.update {
                it.copy(
                    memoryUsageMb = usedMb,
                    activeThreads = threadCount,
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun bindFptnService() {
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? FptnService.LocalBinder ?: return
                subscribeService(binder.service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                unsubscribeService()
            }
        }
        serviceConnection = conn
        try {
            FptnService.bindService(getApplication(), conn)
        } catch (e: Exception) {
            XLog.tag(TAG).w("Failed to bind FptnService: ${e.message}")
        }
    }

    private fun parseSpeedBytesPerSec(speedStr: String): Long {
        // speedStr may look like "1.2 MB/s", "500 KB/s", "120 B/s", "1,2 МБ/с" etc.
        try {
            val parts = speedStr.trim().split(" ")
            if (parts.size >= 2) {
                val num = parts[0].replace(",", ".").toDoubleOrNull() ?: return 0L
                val unit = parts[1].uppercase(Locale.ROOT)
                return when {
                    unit.contains("GB") || unit.contains("ГБ") -> (num * 1_000_000_000L).toLong()
                    unit.contains("MB") || unit.contains("МБ") -> (num * 1_000_000L).toLong()
                    unit.contains("KB") || unit.contains("КБ") -> (num * 1_000L).toLong()
                    else -> num.toLong()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return 0L
    }

    private fun subscribeService(service: FptnService) {
        unsubscribeService()
        subscribedService = service

        speedObserver = Observer { speedAndDuration ->
            if (speedAndDuration != null) {
                val down = formatLocalizedSpeed(speedAndDuration.first)
                val up = formatLocalizedSpeed(speedAndDuration.second)
                val dur = speedAndDuration.third?.toLong() ?: 0L

                val downBps = parseSpeedBytesPerSec(speedAndDuration.first)
                val upBps = parseSpeedBytesPerSec(speedAndDuration.second)
                val currentMax = maxOf(downBps, upBps)
                if (currentMax > maxRecordedSpeedBytesPerSec) {
                    maxRecordedSpeedBytesPerSec = currentMax
                }

                val peakFormatted = if (maxRecordedSpeedBytesPerSec > 0L) {
                    formatSpeedFromBytes(maxRecordedSpeedBytesPerSec)
                } else {
                    "0 Б/с"
                }

                _uiState.update { state ->
                    state.copy(
                        currentDownloadSpeed = down,
                        currentUploadSpeed = up,
                        peakSpeed = peakFormatted,
                        sessionDuration = TimeUtils.getTime(dur),
                    )
                }
            }
        }
        service.getSpeedAndDurationMutableLiveData().observeForever(speedObserver!!)

        trafficObserver = Observer { traffic ->
            if (traffic != null) {
                val downBytes = traffic.first?.toLong() ?: 0L
                val upBytes = traffic.second?.toLong() ?: 0L
                val totalBytes = downBytes + upBytes

                _uiState.update { state ->
                    state.copy(
                        totalDownloaded = formatBytes(downBytes),
                        totalUploaded = formatBytes(upBytes),
                        totalWeeklyTraffic = formatBytes(totalBytes),
                    )
                }
            }
        }
        service.getTrafficBytesLiveData().observeForever(trafficObserver!!)
    }

    private fun formatLocalizedSpeed(raw: String?): String {
        if (raw.isNullOrBlank()) return "0 Б/с"
        return raw.replace("GB/s", "ГБ/с")
            .replace("MB/s", "МБ/с")
            .replace("KB/s", "КБ/с")
            .replace("B/s", "Б/с")
            .replace(".", ",")
    }

    private fun formatSpeedFromBytes(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_000_000_000L -> String.format(Locale("ru", "RU"), "%.1f ГБ/с", bytesPerSec / 1_000_000_000.0)
            bytesPerSec >= 1_000_000L -> String.format(Locale("ru", "RU"), "%.1f МБ/с", bytesPerSec / 1_000_000.0)
            bytesPerSec >= 1_000L -> String.format(Locale("ru", "RU"), "%.1f КБ/с", bytesPerSec / 1_000.0)
            else -> "$bytesPerSec Б/с"
        }
    }

    private fun unsubscribeService() {
        subscribedService?.let { service ->
            speedObserver?.let { service.getSpeedAndDurationMutableLiveData().removeObserver(it) }
            trafficObserver?.let { service.getTrafficBytesLiveData().removeObserver(it) }
        }
        speedObserver = null
        trafficObserver = null
        subscribedService = null
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000L -> String.format(Locale("ru", "RU"), "%.2f ГБ", bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> String.format(Locale("ru", "RU"), "%.2f МБ", bytes / 1_000_000.0)
            bytes >= 1_000L -> String.format(Locale("ru", "RU"), "%.2f КБ", bytes / 1_000.0)
            else -> "$bytes Б"
        }
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeService()
        serviceConnection?.let {
            try {
                getApplication<Application>().unbindService(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
