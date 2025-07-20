package org.fptn.vpn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filantrop.pvnclient.auth.domain.AuthInteractor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.fptn.vpn.core.common.Result
import org.fptn.vpn.core.common.asResult
import org.fptn.vpn.core.model.UserData

class MainViewModel(
    authInteractor: AuthInteractor,
) : ViewModel() {
    val uiState: StateFlow<AuthActivityUiState> =
        authInteractor.userData
            .asResult()
            .map { result ->
                when (result) {
                    is Result.Error -> AuthActivityUiState.Login
                    is Result.Loading -> AuthActivityUiState.Loading
                    is Result.Success -> AuthActivityUiState.Main(result.data)
                }
            }.stateIn(
                scope = viewModelScope,
                initialValue = AuthActivityUiState.Loading,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface AuthActivityUiState {
    data object Loading : AuthActivityUiState

    data object Login : AuthActivityUiState

    data class Main(
        val userData: UserData,
    ) : AuthActivityUiState

    /**
     * Returns `true` if the state wasn't loaded yet and it should keep showing the splash screen.
     */
    fun shouldKeepSplashScreen() = this is Loading
}
