package org.fptn.vpn.views.updatetoken

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.R
import org.fptn.vpn.ui.screens.UpdateTokenScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.settings.SettingsActivity

class UpdateTokenActivity : ComponentActivity() {

    private val viewModel: UpdateTokenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val errorText by viewModel.errorTextLiveData.observeAsState("")
            FptnTheme {
                UpdateTokenScreen(
                    errorText = errorText ?: "",
                    htmlLabel = getString(R.string.telegram_bot_html),
                    onSave = ::onSave,
                    onCancel = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    },
                    onHome = {
                        startActivity(Intent(this, HomeActivity::class.java))
                    },
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                )
            }
        }
    }

    private fun onSave(tokenLink: String) {
        try {
            val future = viewModel.parseAndSaveToken(tokenLink)
            Futures.addCallback(future, object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    Toast.makeText(applicationContext, R.string.token_was_updated, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@UpdateTokenActivity, SettingsActivity::class.java))
                    finish()
                }

                override fun onFailure(t: Throwable) {
                    XLog.tag("UpdateTokenActivity").e("Token update failed: %s", t.message)
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    viewModel.errorTextLiveData.postValue(t.message)
                }
            }, mainExecutor)
        } catch (e: Exception) {
            XLog.tag("UpdateTokenActivity").e("Token parsing failed: %s", e.message)
            Toast.makeText(applicationContext, R.string.token_saving_failed, Toast.LENGTH_SHORT).show()
            viewModel.errorTextLiveData.postValue(getString(R.string.token_saving_failed))
        }
    }
}
