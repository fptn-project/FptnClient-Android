package org.fptn.vpn.ui.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.ui.home.HomeActivityViewModel
import org.fptn.vpn.utils.CountryFlags
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.token.TokenUtils
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServersViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ServersViewModel"
    private val appDatabase = AppDatabase.getInstance(application)
    private val serverDAO = appDatabase.serverDAO()

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    init {
        loadServersFromDb()
    }

    fun loadServersFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbServers = serverDAO.serverList
                val selectedServer = serverDAO.selected
                val isAutoSelected = selectedServer == null || selectedServer.IsAuto()

                // Preserve or copy previous pings if available
                HomeActivityViewModel.lastPingedServers?.let { pingedList ->
                    for (loaded in dbServers) {
                        for (pinged in pingedList) {
                            if (loaded.host == pinged.host && loaded.port == pinged.port) {
                                loaded.pingMs = pinged.pingMs
                                break
                            }
                        }
                    }
                }

                val tokenDateMillis = SharedPrefUtils.getTokenUpdatedDate(getApplication())
                val formattedDate = if (tokenDateMillis > 0L) {
                    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    sdf.format(Date(tokenDateMillis))
                } else {
                    "Недавно"
                }

                // Minimum ping for auto-select node
                val validPings = dbServers.filter { it.pingMs > 0 }.map { it.pingMs }
                val minPing = if (validPings.isNotEmpty()) validPings.minOrNull()!!.toInt() else 35

                val autoNode = ServerItem(
                    id = "srv_auto",
                    name = "Авто-выбор (Самый быстрый)",
                    region = "Автоматический маршрут",
                    flag = "⚡",
                    pingMs = minPing,
                    protocol = "fptn",
                    port = 443,
                    isAutoSelect = true,
                    isSelected = isAutoSelected,
                )

                val regularItems = dbServers.map { server ->
                    val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode) ?: "🌐"
                    val isSelected = !isAutoSelected && selectedServer != null && selectedServer.id == server.id
                    ServerItem(
                        id = server.id.toString(),
                        name = server.name,
                        region = server.host,
                        flag = flag,
                        pingMs = if (server.pingMs >= 0) server.pingMs.toInt() else -1,
                        protocol = "fptn",
                        port = server.port,
                        isAutoSelect = false,
                        isSelected = isSelected,
                        isOnline = server.pingMs >= 0 || server.pingMs == -1L,
                    )
                }

                val allServers = listOf(autoNode) + regularItems

                val profile = ServerProfile(
                    id = "fptn_active_profile",
                    title = "FPTN VPN Profile",
                    emoji = "⚡",
                    updatedAt = formattedDate,
                    trafficUsed = "Активен",
                    trafficLimit = "${dbServers.size} узлов",
                    trafficProgress = 1.0f,
                    daysLeftText = "${dbServers.size} доступных серверов",
                    isSelected = true,
                )

                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            profiles = listOf(profile),
                            servers = allServers,
                        )
                    }
                }
            } catch (e: Exception) {
                XLog.tag(TAG).e("Error loading servers: ${e.message}")
            }
        }
    }

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

    fun refreshProfile() {
        if (_uiState.value.isRefreshingProfile) return
        _uiState.update { it.copy(isRefreshingProfile = true) }

        viewModelScope.launch {
            delay(500)
            loadServersFromDb()
            _uiState.update { it.copy(isRefreshingProfile = false) }
        }
    }

    fun selectServer(serverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (serverId == "srv_auto") {
                    serverDAO.resetSelected()
                    SharedPrefUtils.saveResetSelectedServerEnabled(getApplication(), true)
                } else {
                    val id = serverId.toIntOrNull()
                    if (id != null) {
                        serverDAO.setSelected(id)
                    }
                }
                loadServersFromDb()
            } catch (e: Exception) {
                XLog.tag(TAG).e("Failed to select server $serverId: ${e.message}")
            }
        }
    }

    fun setSortFilter(filter: ServerSortFilter) {
        _uiState.update { it.copy(sortFilter = filter) }
    }

    fun checkPing() {
        if (_uiState.value.isCheckingPing) return
        _uiState.update { it.copy(isCheckingPing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbServers = serverDAO.serverList
                val pingJobs = dbServers.map { server ->
                    async {
                        val startTime = System.currentTimeMillis()
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(server.host, server.port), 4000)
                                server.pingMs = System.currentTimeMillis() - startTime
                            }
                        } catch (e: Exception) {
                            server.pingMs = -1
                        }
                        server
                    }
                }
                pingJobs.awaitAll()
                HomeActivityViewModel.lastPingedServers = dbServers
                loadServersFromDb()
            } catch (e: Exception) {
                XLog.tag(TAG).e("Ping check error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isCheckingPing = false) }
                }
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

        viewModelScope.launch {
            try {
                val serverEntities = withContext(Dispatchers.IO) {
                    TokenUtils.parseToken(input)
                }
                if (serverEntities == null || serverEntities.isEmpty()) {
                    _uiState.update { it.copy(tokenInputError = "В токене не найдено серверов") }
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    serverDAO.deleteAndInsert(serverEntities)
                    SharedPrefUtils.saveTokenUpdatedDate(getApplication(), System.currentTimeMillis())
                }

                _uiState.update {
                    it.copy(
                        showAddTokenSheet = false,
                        tokenInputText = "",
                        tokenInputError = null,
                    )
                }
                loadServersFromDb()
            } catch (e: Exception) {
                XLog.tag(TAG).e("Error parsing token: ${e.message}")
                _uiState.update { it.copy(tokenInputError = e.localizedMessage ?: "Неверный формат токена") }
            }
        }
    }
}
