package org.fptn.vpn.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.ConnectionStrategy

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setConnectionStrategy(strategy: ConnectionStrategy) {
        _uiState.update { it.copy(connectionStrategy = strategy) }
    }

    fun setBypassMethod(method: BypassCensorshipMethod) {
        _uiState.update { it.copy(bypassMethod = method) }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(adBlockEnabled = enabled) }
    }

    fun setDomainBlacklistEnabled(enabled: Boolean) {
        _uiState.update { it.copy(domainBlacklistEnabled = enabled) }
    }

    fun setCustomDnsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(customDnsEnabled = enabled) }
    }

    fun setCustomDnsIpv4(dns: String) {
        _uiState.update { it.copy(customDnsIpv4 = dns) }
    }

    fun setShowSpeedInNotification(show: Boolean) {
        _uiState.update { it.copy(showSpeedInNotification = show) }
    }

    fun setShowTrafficInNotification(show: Boolean) {
        _uiState.update { it.copy(showTrafficInNotification = show) }
    }

    fun setAutoFallbackEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoFallbackEnabled = enabled) }
    }

    fun setAutoFallbackThreshold(threshold: Int) {
        _uiState.update { it.copy(autoFallbackThreshold = threshold) }
    }

    fun setReconnectAttemptsCount(count: Int) {
        _uiState.update { it.copy(reconnectAttemptsCount = count) }
    }

    fun setDelayBetweenReconnect(seconds: Int) {
        _uiState.update { it.copy(delayBetweenReconnect = seconds) }
    }

    fun setReconnectOnChangeNetworkType(enabled: Boolean) {
        _uiState.update { it.copy(reconnectOnChangeNetworkType = enabled) }
    }

    fun setReconnectOnChangeIP(enabled: Boolean) {
        _uiState.update { it.copy(reconnectOnChangeIP = enabled) }
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(killSwitchEnabled = enabled) }
    }

    fun setResetSelectedServerOnDisconnect(enabled: Boolean) {
        _uiState.update { it.copy(resetSelectedServerOnDisconnect = enabled) }
    }

    fun setFontScale(scale: Float) {
        _uiState.update { it.copy(fontScale = scale) }
    }

    fun toggleAppearanceExpanded() {
        _uiState.update { it.copy(isAppearanceExpanded = !it.isAppearanceExpanded) }
    }

    fun openSniDialog() {
        _uiState.update { it.copy(showSniDialog = true) }
    }

    fun closeSniDialog() {
        _uiState.update { it.copy(showSniDialog = false) }
    }

    fun updateSni(sni: String) {
        _uiState.update { it.copy(currentSni = sni, showSniDialog = false) }
    }

    fun openBlacklistDialog() {
        _uiState.update { it.copy(showBlacklistDialog = true) }
    }

    fun closeBlacklistDialog() {
        _uiState.update { it.copy(showBlacklistDialog = false) }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirm = true) }
    }

    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirm = false) }
    }

    fun checkUpdates() {
        if (_uiState.value.isCheckingUpdates) return
        _uiState.update { it.copy(isCheckingUpdates = true) }
        viewModelScope.launch {
            delay(800)
            _uiState.update { it.copy(isCheckingUpdates = false) }
        }
    }

    fun deleteAllServers() {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.serverDAO().deleteAll()
        }
    }
}
