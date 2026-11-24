package org.fptn.vpn.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fptn.vpn.enums.ConnectionState
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionDurationSeconds: Long = 0L

    init {
        updateGreeting()
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

    fun toggleConnection() {
        when (_uiState.value.connectionState) {
            ConnectionState.DISCONNECTED -> startConnecting()
            ConnectionState.CONNECTING, ConnectionState.CONNECTED -> disconnect()
            else -> disconnect()
        }
    }

    private fun startConnecting() {
        timerJob?.cancel()
        _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING) }

        viewModelScope.launch {
            // Mock connection handshake delay
            delay(1200)
            sessionDurationSeconds = 0L
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.CONNECTED,
                    sessionTimerText = formatTimer(sessionDurationSeconds),
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                sessionDurationSeconds++
                _uiState.update { it.copy(sessionTimerText = formatTimer(sessionDurationSeconds)) }
            }
        }
    }

    private fun disconnect() {
        timerJob?.cancel()
        sessionDurationSeconds = 0L
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED,
                sessionTimerText = "00:00:00",
            )
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(isNoticeVisible = false) }
    }

    private fun formatTimer(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
