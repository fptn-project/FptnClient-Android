package org.fptn.vpn.views.bypassmethod

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.ui.screens.BypassMethodsScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.settings.SettingsActivity

class BypassMethodsActivity : ComponentActivity() {

    private val tag = "BypassMethodsActivity"
    private val viewModel: BypassMethodsViewModel by viewModels()
    private var connection: ServiceConnection? = null

    override fun onStart() {
        super.onStart()
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                XLog.tag(tag).i("SNI checker service connected")
                val binder = service as SniCheckerService.LocalBinder
                viewModel.subscribeService(binder.service)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                viewModel.unsubscribe()
            }
        }
        SniCheckerService.bindService(this, connection)
    }

    override fun onStop() {
        super.onStop()
        try { connection?.let { unbindService(it) } } catch (e: Exception) {
            XLog.tag(tag).e("Error unbinding: %s", e.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try { viewModel.loadDefaultSni() } catch (e: Exception) {
            XLog.tag(tag).w("Failed to load default SNI: %s", e.message)
        }

        setContent {
            val bypassMethod by viewModel.bypassCensorshipMethodMutableLiveData.observeAsState()
            val sniMode by viewModel.sniSpoofingModeMutableLiveData.observeAsState()
            val sniText by viewModel.sniMutableLiveData.observeAsState("")
            val sniCount by viewModel.sniCountLiveData.observeAsState(0)
            val serviceState by viewModel.serviceState.observeAsState(SniCheckerServiceState.INACTIVE)
            val progress by viewModel.currentProgress.observeAsState(android.util.Pair.create(0, 1))
            val currentSni by viewModel.currentCheckingSniInfo.observeAsState("")
            val selectedServer by viewModel.selectedServer.observeAsState(ServerEntity.AUTO)
            val allServersState by viewModel.allServersCacheLiveData.observeAsState(emptyList())

            FptnTheme {
                BypassMethodsScreen(
                    bypassMethod = bypassMethod ?: viewModel.bypassCensorshipMethodMutableLiveData.value!!,
                    sniSpoofingMode = sniMode ?: viewModel.sniSpoofingModeMutableLiveData.value!!,
                    sniText = sniText ?: "",
                    sniCount = sniCount ?: 0,
                    serviceState = serviceState,
                    progress = Pair(progress?.first ?: 0, progress?.second ?: 1),
                    currentCheckingSni = currentSni ?: "",
                    selectedServer = selectedServer ?: ServerEntity.AUTO,
                    allServers = allServersState ?: emptyList(),
                    onBypassMethodChange = { viewModel.setBypassMethod(it) },
                    onSniSpoofingModeChange = { viewModel.setSniSpoofingMode(it) },
                    onEditSni = { viewModel.validateAndSetSni(it) },
                    onSave = {
                        viewModel.saveBypassMethod()
                        finish()
                    },
                    onCancel = { finish() },
                    onStartAutoSelect = { server, reset ->
                        SniCheckerService.startChecking(
                            this,
                            server,
                            reset,
                            viewModel.bypassCensorshipMethodMutableLiveData.value
                        )
                    },
                    onStopAutoSelect = { SniCheckerService.stopChecking(this) },
                    onHome = { startActivity(Intent(this, HomeActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }

        loadAllServers()
    }

    private fun loadAllServers() {
        Futures.addCallback(viewModel.getAllServers(), object : FutureCallback<List<ServerEntity>?> {
            override fun onSuccess(result: List<ServerEntity>?) {
                viewModel.allServersCacheLiveData.postValue(result ?: emptyList())
            }
            override fun onFailure(t: Throwable) {
                XLog.tag(tag).e("Failed to load servers: %s", t.message)
            }
        }, mainExecutor)
    }
}
