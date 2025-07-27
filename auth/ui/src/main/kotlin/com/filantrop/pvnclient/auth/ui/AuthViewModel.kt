package com.filantrop.pvnclient.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fptn.vpn.auth.domain.AuthInteractor

data class AuthState(
    val token: String,
)

class AuthViewModel(
    private val authInteractor: AuthInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthState(""))
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun changeToken(token: String) {
        _uiState.update { it.copy(token = token) }
    }

    fun login() =
        viewModelScope.launch {
            authInteractor.saveToken(_uiState.value.token)
        }
}
