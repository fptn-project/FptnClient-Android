package org.fptn.vpn.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class StatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        // Coroutine simulating live telemetry speed variations
        viewModelScope.launch {
            while (true) {
                delay(1200)
                _uiState.update { state ->
                    val jitterDown = Random.nextDouble(16.5, 21.2)
                    val jitterUp = Random.nextDouble(3.8, 5.1)
                    state.copy(
                        currentDownloadSpeed = String.format("%.1f MB/s", jitterDown),
                        currentUploadSpeed = String.format("%.1f MB/s", jitterUp),
                    )
                }
            }
        }
    }
}
