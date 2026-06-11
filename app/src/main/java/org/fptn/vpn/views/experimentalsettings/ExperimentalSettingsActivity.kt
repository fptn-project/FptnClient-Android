package org.fptn.vpn.views.experimentalsettings

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.elvishew.xlog.XLog
import org.fptn.vpn.R
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.ui.screens.ExperimentalSettings
import org.fptn.vpn.ui.screens.ExperimentalSettingsScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.utils.SharedPrefUtils

class ExperimentalSettingsActivity : ComponentActivity() {

    private val tag = "ExperimentalSettingsActivity"
    private val attemptsValues = intArrayOf(5, 15, 35, Int.MAX_VALUE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reconnectAttempts = SharedPrefUtils.getReconnectAttemptsCount(this)
        val attemptsProgress = attemptsValues.indexOfFirst { it >= reconnectAttempts }.takeIf { it >= 0 } ?: 2
        val delayProgress = SharedPrefUtils.getDelayBetweenReconnect(this) - 1

        val initial = ExperimentalSettings(
            reconnectOnNetworkChange = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this),
            reconnectOnIpChange = SharedPrefUtils.getReconnectOnChangeIPEnabled(this),
            attemptsProgress = attemptsProgress,
            delayProgress = delayProgress,
            resetServerOnDisconnect = SharedPrefUtils.getResetSelectedServerEnabled(this),
            resetServerOnException = SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(this),
        )

        setContent {
            FptnTheme {
                ExperimentalSettingsScreen(
                    initial = initial,
                    showTileButton = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    onRequestTile = { requestQuickSettingsTile() },
                    onSave = { settings -> saveAndFinish(settings) },
                    onCancel = { finish() },
                )
            }
        }
    }

    private fun saveAndFinish(settings: ExperimentalSettings) {
        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, settings.reconnectOnNetworkChange)
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, settings.reconnectOnIpChange)
        SharedPrefUtils.saveResetSelectedServerEnabled(this, settings.resetServerOnDisconnect)
        SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(this, settings.resetServerOnException)
        SharedPrefUtils.saveReconnectAttemptsCount(this, attemptsValues[settings.attemptsProgress])
        SharedPrefUtils.saveDelayBetweenReconnect(this, settings.delayProgress + 1)

        XLog.tag(tag).i(
            "Experimental settings saved [watchNetwork=%b, watchIP=%b, attempts=%d, delay=%ds]",
            settings.reconnectOnNetworkChange,
            settings.reconnectOnIpChange,
            attemptsValues[settings.attemptsProgress],
            settings.delayProgress + 1,
        )
        finish()
    }

    @SuppressLint("InlinedApi")
    private fun requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBarManager = getSystemService(StatusBarManager::class.java) ?: return
        try {
            statusBarManager.requestAddTileService(
                ComponentName(this, FptnTileService::class.java),
                getString(R.string.app_name),
                Icon.createWithResource(this, R.drawable.ic_logo),
                mainExecutor
            ) { resultCode ->
                when (resultCode) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show()
                    else ->
                        XLog.tag(tag).w("Tile request result: %d", resultCode)
                }
            }
        } catch (e: Exception) {
            XLog.tag(tag).e("Failed to request tile: %s", e.message)
            Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
