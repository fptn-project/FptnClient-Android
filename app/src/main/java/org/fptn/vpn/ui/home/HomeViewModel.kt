package org.fptn.vpn.ui.home

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.services.vpn.FptnService
import org.fptn.vpn.services.vpn.FptnServiceState
import org.fptn.vpn.utils.CountryFlags
import org.fptn.vpn.utils.TimeUtils
import java.util.Calendar
import java.util.Locale

import org.fptn.vpn.ui.home.HomeActivityViewModel

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "HomeViewModel"
    private val appDatabase = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionDurationSeconds: Long = 0L

    private var subscribedService: FptnService? = null
    private var serviceConnection: ServiceConnection? = null

    // Observers for FptnService LiveData
    private var speedAndDurationObserver: Observer<Triple<String, String, Long>>? = null
    private var trafficBytesObserver: Observer<Pair<Long, Long>>? = null
    private var serviceStateObserver: Observer<FptnServiceState>? = null

    // Observer for static tile service state (always active across processes/services)
    private val tileStateObserver = Observer<ConnectionState> { connectionState ->
        val state = connectionState ?: ConnectionState.DISCONNECTED
        _uiState.update { it.copy(connectionState = state) }
        when (state) {
            ConnectionState.CONNECTED -> {
                startTimer()
            }
            ConnectionState.DISCONNECTED -> {
                stopTimer()
                loadActiveServer()
            }
            else -> {}
        }
    }

    init {
        updateGreeting()
        loadActiveServer()

        // Observe static connection state
        FptnTileService.getServiceStateMutableLiveData().observeForever(tileStateObserver)

        // Bind to FptnService
        bindFptnService()
    }

    fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingText = when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
        _uiState.update { it.copy(greeting = greetingText) }
    }

    fun loadActiveServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selected = appDatabase.serverDAO().selected
                val isOnline = _uiState.value.connectionState == ConnectionState.CONNECTED
                val pingedList = HomeActivityViewModel.lastPingedServers

                withContext(Dispatchers.Main) {
                    if (selected != null && !selected.IsAuto()) {
                        val flag = CountryFlags.getCountryFlagByCountryCode(selected.countryCode) ?: "🌐"
                        val cachedPing = pingedList?.firstOrNull { it.host == selected.host && it.port == selected.port }?.pingMs ?: selected.pingMs
                        val ping = if (cachedPing > 0) cachedPing.toInt() else -1
                        _uiState.update {
                            it.copy(
                                serverName = selected.name,
                                serverRegion = selected.host,
                                serverFlag = flag,
                                serverProtocol = "fptn",
                                pingMs = ping,
                                services = buildServicePings(ping, isOnline),
                            )
                        }
                    } else {
                        // Auto-selected server: find lowest valid ping among tested servers
                        val validPings = pingedList?.filter { it.pingMs > 0 }?.map { it.pingMs }
                        val minPing = if (!validPings.isNullOrEmpty()) validPings.minOrNull()!!.toInt() else -1
                        _uiState.update {
                            it.copy(
                                serverName = "Авто-выбор",
                                serverRegion = "Оптимальный маршрут",
                                serverFlag = "⚡",
                                serverProtocol = "fptn",
                                pingMs = minPing,
                                services = buildServicePings(minPing, isOnline),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                XLog.tag(TAG).e("Error loading active server: ${e.message}")
            }
        }
    }

    private fun buildServicePings(basePing: Int, isOnline: Boolean): List<ServicePing> {
        val hasPing = basePing > 0
        return listOf(
            ServicePing("YouTube", if (isOnline) "Доступен" else "Ожидание", isOnline, if (hasPing) (basePing * 0.9).toInt().coerceAtLeast(15) else -1),
            ServicePing("Telegram", if (isOnline) "Доступен" else "Ожидание", isOnline, if (hasPing) (basePing * 0.8).toInt().coerceAtLeast(12) else -1),
            ServicePing("Discord", if (isOnline) "Доступен" else "Ожидание", isOnline, if (hasPing) (basePing * 1.1).toInt().coerceAtLeast(20) else -1),
            ServicePing("OpenAI", if (isOnline) "Доступен" else "Ожидание", isOnline, if (hasPing) (basePing * 1.25).toInt().coerceAtLeast(25) else -1),
        )
    }

    fun startVpn() {
        viewModelScope.launch(Dispatchers.IO) {
            val selected = appDatabase.serverDAO().selected
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING) }
                if (selected != null && !selected.IsAuto()) {
                    FptnService.startToConnect(getApplication(), selected)
                } else {
                    FptnService.startToConnect(getApplication())
                }
            }
        }
    }

    fun disconnectVpn() {
        FptnService.startToDisconnect(getApplication())
    }

    fun toggleConnection() {
        when (_uiState.value.connectionState) {
            ConnectionState.DISCONNECTED -> startVpn()
            else -> disconnectVpn()
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

    private fun subscribeService(service: FptnService) {
        unsubscribeService()
        subscribedService = service

        speedAndDurationObserver = Observer { speedAndDuration ->
            if (speedAndDuration != null) {
                val down = formatSpeed(speedAndDuration.first)
                val up = formatSpeed(speedAndDuration.second)
                val dur = speedAndDuration.third?.toLong() ?: 0L
                sessionDurationSeconds = dur
                _uiState.update {
                    it.copy(
                        downloadSpeed = down,
                        uploadSpeed = up,
                        sessionTimerText = TimeUtils.getTime(sessionDurationSeconds),
                    )
                }
            }
        }
        service.getSpeedAndDurationMutableLiveData().observeForever(speedAndDurationObserver!!)

        trafficBytesObserver = Observer { traffic ->
            if (traffic != null) {
                val rx = traffic.first?.toLong() ?: 0L
                val tx = traffic.second?.toLong() ?: 0L
                _uiState.update {
                    it.copy(
                        downloadTraffic = formatBytes(rx),
                        uploadTraffic = formatBytes(tx),
                    )
                }
            }
        }
        service.getTrafficBytesLiveData().observeForever(trafficBytesObserver!!)

        serviceStateObserver = Observer { serviceState ->
            if (serviceState != null) {
                val connState = serviceState.connectionState
                if (connState != null) {
                    _uiState.update { it.copy(connectionState = connState) }
                }
            }
        }
        service.getServiceStateMutableLiveData().observeForever(serviceStateObserver!!)
    }

    private fun unsubscribeService() {
        subscribedService?.let { service ->
            speedAndDurationObserver?.let { service.getSpeedAndDurationMutableLiveData().removeObserver(it) }
            trafficBytesObserver?.let { service.getTrafficBytesLiveData().removeObserver(it) }
            serviceStateObserver?.let { service.getServiceStateMutableLiveData().removeObserver(it) }
        }
        speedAndDurationObserver = null
        trafficBytesObserver = null
        serviceStateObserver = null
        subscribedService = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                sessionDurationSeconds++
                _uiState.update { it.copy(sessionTimerText = TimeUtils.getTime(sessionDurationSeconds)) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        sessionDurationSeconds = 0L
        _uiState.update {
            it.copy(sessionTimerText = "00:00:00")
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(isNoticeVisible = false) }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000L -> String.format(Locale("ru", "RU"), "%.1f ГБ", bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> String.format(Locale("ru", "RU"), "%.1f МБ", bytes / 1_000_000.0)
            bytes >= 1_000L -> String.format(Locale("ru", "RU"), "%.1f КБ", bytes / 1_000.0)
            else -> "$bytes Б"
        }
    }

    private fun formatSpeed(speed: String?): String {
        if (speed.isNullOrBlank()) return "0 Б/с"
        return speed
            .replace(".", ",")
            .replace("MB/s", "МБ/с")
            .replace("KB/s", "КБ/с")
            .replace("B/s", "Б/с")
            .replace("Gbps", "Гбит/с")
            .replace("Mbps", "Мбит/с")
            .replace("Kbps", "Кбит/с")
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        FptnTileService.getServiceStateMutableLiveData().removeObserver(tileStateObserver)
        unsubscribeService()
        serviceConnection?.let {
            try {
                getApplication<Application>().unbindService(it)
            } catch (e: Exception) {
                // Ignore if already unbound
            }
        }
    }
}
