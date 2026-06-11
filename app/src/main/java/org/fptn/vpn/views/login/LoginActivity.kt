package org.fptn.vpn.views.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.elvishew.xlog.XLog
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.R
import org.fptn.vpn.ui.screens.LoginScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.home.HomeActivity

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { /* disable back */ }

        setContent {
            val errorText by viewModel.errorTextLiveData.observeAsState("")
            FptnTheme {
                LoginScreen(
                    errorText = errorText ?: "",
                    htmlLabel = getString(R.string.telegram_bot_html),
                    onLogin = ::onLogin,
                )
            }
        }
    }

    private fun onLogin(tokenLink: String) {
        try {
            val future = viewModel.parseAndSaveToken(tokenLink)
            Futures.addCallback(future, object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }

                override fun onFailure(t: Throwable) {
                    XLog.tag("LoginActivity").e("Login failed: %s", t.message)
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    viewModel.errorTextLiveData.postValue(t.message)
                }
            }, mainExecutor)
        } catch (e: Exception) {
            XLog.tag("LoginActivity").e("Token parsing failed: %s", e.message)
            Toast.makeText(applicationContext, R.string.token_saving_failed, Toast.LENGTH_SHORT).show()
            viewModel.errorTextLiveData.postValue(getString(R.string.token_saving_failed))
        }
    }
}
