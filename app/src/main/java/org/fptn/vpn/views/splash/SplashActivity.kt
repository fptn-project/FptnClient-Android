package org.fptn.vpn.views.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.ui.screens.SplashScreen
import org.fptn.vpn.ui.theme.FptnTheme
import org.fptn.vpn.views.bypassmethod.BypassMethodsActivity
import org.fptn.vpn.views.home.HomeActivity
import org.fptn.vpn.views.login.LoginActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FptnTheme { SplashScreen() }
        }
        navigate()
    }

    private fun navigate() {
        val countFuture = AppDatabase.getInstance(this).serverDAO().getCount()
        Futures.addCallback(countFuture, object : FutureCallback<Int?> {
            override fun onSuccess(count: Int?) {
                val sniActive = SniCheckerService.getStaticServiceState().value == SniCheckerServiceState.ACTIVE
                val intent = when {
                    (count ?: 0) > 0 && sniActive ->
                        Intent(this@SplashActivity, BypassMethodsActivity::class.java)
                    (count ?: 0) > 0 ->
                        Intent(this@SplashActivity, HomeActivity::class.java)
                    else ->
                        Intent(this@SplashActivity, LoginActivity::class.java)
                }
                startActivity(intent)
                finish()
            }

            override fun onFailure(t: Throwable) {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }, mainExecutor)
    }
}
