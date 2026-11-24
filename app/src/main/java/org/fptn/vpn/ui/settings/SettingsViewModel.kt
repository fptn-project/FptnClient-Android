package org.fptn.vpn.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fptn.vpn.BuildConfig
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.ConnectionStrategy
import org.fptn.vpn.utils.PermissionsUtils
import org.fptn.vpn.utils.SharedPrefUtils

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SettingsViewModel"
    private val appDatabase = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val app = getApplication<Application>()
        _uiState.update { state ->
            state.copy(
                connectionStrategy = SharedPrefUtils.getConnectionStrategy(app),
                bypassMethod = SharedPrefUtils.getBypassCensorshipMethod(app),
                currentSni = SharedPrefUtils.getSniHostname(app),
                perAppMode = SharedPrefUtils.getPerAppVPNMode(app),
                adBlockEnabled = SharedPrefUtils.getAdBlockEnabled(app),
                domainBlacklistEnabled = SharedPrefUtils.getDomainBlacklistEnabled(app),
                customDnsEnabled = SharedPrefUtils.getCustomDnsEnabled(app),
                customDnsIpv4 = SharedPrefUtils.getCustomDnsIpv4(app).ifBlank { "1.1.1.1" },
                showSpeedInNotification = SharedPrefUtils.getShowSpeedInNotification(app),
                showTrafficInNotification = SharedPrefUtils.getShowTrafficInNotification(app),
                autoFallbackEnabled = SharedPrefUtils.getAutoFallbackEnabled(app),
                autoFallbackThreshold = SharedPrefUtils.getAutoFallbackThreshold(app),
                reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(app),
                delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(app),
                reconnectOnChangeNetworkType = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(app),
                reconnectOnChangeIP = SharedPrefUtils.getReconnectOnChangeIPEnabled(app),
                killSwitchEnabled = SharedPrefUtils.getKillSwitchEnabled(app),
                resetSelectedServerOnDisconnect = SharedPrefUtils.getResetSelectedServerEnabled(app),
                batteryOptimizationGranted = PermissionsUtils.checkBatteryOptimizations(app),
                appVersion = BuildConfig.VERSION_NAME,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val excludedCount = appDatabase.appInfoDAO().all.size
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(excludedAppsCount = excludedCount) }
                }
            } catch (e: Exception) {
                XLog.tag(TAG).e("Error reading appInfo count: ${e.message}")
            }
        }
    }

    fun setConnectionStrategy(strategy: ConnectionStrategy) {
        SharedPrefUtils.saveConnectionStrategy(getApplication(), strategy)
        _uiState.update { it.copy(connectionStrategy = strategy) }
    }

    fun setBypassMethod(method: BypassCensorshipMethod) {
        SharedPrefUtils.saveBypassCensorshipMethod(getApplication(), method)
        _uiState.update { it.copy(bypassMethod = method) }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        SharedPrefUtils.saveAdBlockEnabled(getApplication(), enabled)
        _uiState.update { it.copy(adBlockEnabled = enabled) }
    }

    fun setDomainBlacklistEnabled(enabled: Boolean) {
        SharedPrefUtils.saveDomainBlacklistEnabled(getApplication(), enabled)
        _uiState.update { it.copy(domainBlacklistEnabled = enabled) }
    }

    fun setCustomDnsEnabled(enabled: Boolean) {
        SharedPrefUtils.saveCustomDnsEnabled(getApplication(), enabled)
        _uiState.update { it.copy(customDnsEnabled = enabled) }
    }

    fun setCustomDnsIpv4(dns: String) {
        SharedPrefUtils.saveCustomDnsIpv4(getApplication(), dns)
        _uiState.update { it.copy(customDnsIpv4 = dns) }
    }

    fun setShowSpeedInNotification(show: Boolean) {
        SharedPrefUtils.saveShowSpeedInNotification(getApplication(), show)
        _uiState.update { it.copy(showSpeedInNotification = show) }
    }

    fun setShowTrafficInNotification(show: Boolean) {
        SharedPrefUtils.saveShowTrafficInNotification(getApplication(), show)
        _uiState.update { it.copy(showTrafficInNotification = show) }
    }

    fun setAutoFallbackEnabled(enabled: Boolean) {
        SharedPrefUtils.saveAutoFallbackEnabled(getApplication(), enabled)
        _uiState.update { it.copy(autoFallbackEnabled = enabled) }
    }

    fun setAutoFallbackThreshold(threshold: Int) {
        SharedPrefUtils.saveAutoFallbackThreshold(getApplication(), threshold)
        _uiState.update { it.copy(autoFallbackThreshold = threshold) }
    }

    fun setReconnectAttemptsCount(count: Int) {
        SharedPrefUtils.saveReconnectAttemptsCount(getApplication(), count)
        _uiState.update { it.copy(reconnectAttemptsCount = count) }
    }

    fun setDelayBetweenReconnect(seconds: Int) {
        SharedPrefUtils.saveDelayBetweenReconnect(getApplication(), seconds)
        _uiState.update { it.copy(delayBetweenReconnect = seconds) }
    }

    fun setReconnectOnChangeNetworkType(enabled: Boolean) {
        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(getApplication(), enabled)
        _uiState.update { it.copy(reconnectOnChangeNetworkType = enabled) }
    }

    fun setReconnectOnChangeIP(enabled: Boolean) {
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(getApplication(), enabled)
        _uiState.update { it.copy(reconnectOnChangeIP = enabled) }
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        SharedPrefUtils.saveKillSwitchEnabled(getApplication(), enabled)
        _uiState.update { it.copy(killSwitchEnabled = enabled) }
    }

    fun setResetSelectedServerOnDisconnect(enabled: Boolean) {
        SharedPrefUtils.saveResetSelectedServerEnabled(getApplication(), enabled)
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
        SharedPrefUtils.saveSniHostname(getApplication(), sni)
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
