package org.fptn.vpn.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class ServersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    fun toggleProfilesExpanded() {
        _uiState.update { it.copy(isProfilesExpanded = !it.isProfilesExpanded) }
    }

    fun selectProfile(profileId: String) {
        _uiState.update { state ->
            state.copy(
                profiles = state.profiles.map { it.copy(isSelected = (it.id == profileId)) },
                isProfilesExpanded = false,
            )
        }
    }

    fun selectServer(serverId: String) {
        _uiState.update { state ->
            state.copy(
                servers = state.servers.map { it.copy(isSelected = (it.id == serverId)) },
            )
        }
    }

    fun setSortFilter(filter: ServerSortFilter) {
        _uiState.update { it.copy(sortFilter = filter) }
    }

    fun checkPing() {
        if (_uiState.value.isCheckingPing) return
        _uiState.update { it.copy(isCheckingPing = true) }

        viewModelScope.launch {
            delay(800) // Mock latency test delay
            _uiState.update { state ->
                val updatedServers = state.servers.map { server ->
                    val jitter = Random.nextInt(-8, 12)
                    val newPing = (server.pingMs + jitter).coerceIn(24, 320)
                    server.copy(pingMs = newPing)
                }
                state.copy(
                    servers = updatedServers,
                    isCheckingPing = false,
                )
            }
        }
    }

    fun openAddTokenSheet() {
        _uiState.update { it.copy(showAddTokenSheet = true, tokenInputText = "", tokenInputError = null) }
    }

    fun closeAddTokenSheet() {
        _uiState.update { it.copy(showAddTokenSheet = false, tokenInputText = "", tokenInputError = null) }
    }

    fun updateTokenInput(text: String) {
        _uiState.update { it.copy(tokenInputText = text, tokenInputError = null) }
    }

    fun submitToken() {
        val input = _uiState.value.tokenInputText.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(tokenInputError = "Токен не может быть пустым") }
            return
        }

        val newProfileId = "profile_${System.currentTimeMillis()}"
        val newServerId = "srv_${System.currentTimeMillis()}"
        val newProfileName = if (input.contains("http", ignoreCase = true)) "FPTN Web Profile" else "FPTN Custom Key"

        val newProfile = ServerProfile(
            id = newProfileId,
            title = newProfileName,
            emoji = "✨",
            updatedAt = "Только что",
            trafficUsed = "0 B",
            trafficLimit = "50,0 GiB",
            trafficProgress = 0.01f,
            daysLeftText = "Осталось 30 дней",
            isSelected = true,
        )

        val newServer = ServerItem(
            id = newServerId,
            name = "Новый узел ($newProfileName)",
            region = "Автоматический",
            flag = "🌐",
            pingMs = Random.nextInt(32, 75),
            protocol = "fptn",
            port = 443,
            isSelected = true,
        )

        _uiState.update { state ->
            state.copy(
                profiles = listOf(newProfile) + state.profiles.map { it.copy(isSelected = false) },
                servers = listOf(newServer) + state.servers.map { it.copy(isSelected = false) },
                showAddTokenSheet = false,
                tokenInputText = "",
                tokenInputError = null,
            )
        }
    }
}
