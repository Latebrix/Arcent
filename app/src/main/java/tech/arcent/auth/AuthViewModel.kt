package tech.arcent.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.arcent.auth.data.AuthRepository
import tech.arcent.auth.data.AppwriteAuthRepository

/*
 * ViewModel for managing authentication state and events.
 */

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isChecking: Boolean = true
)

sealed class AuthEvent {
    data class GoogleToken(val idToken: String): AuthEvent()
    data class GoogleFailed(val statusCode: Int, val msg: String?): AuthEvent()
    object LocalRequested: AuthEvent()
    object CheckSession: AuthEvent()
}

class AuthViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val repo: AuthRepository = AppwriteAuthRepository()

    fun onEvent(e: AuthEvent, context: Context? = null) {
        when(e) {
            is AuthEvent.GoogleToken -> googleLogin(e.idToken, context)
            is AuthEvent.GoogleFailed -> _uiState.update { it.copy(error = "google_${e.statusCode}:${e.msg}", isLoading = false) }
            AuthEvent.LocalRequested -> _uiState.update { it.copy(isAuthenticated = true, error = null) }
            AuthEvent.CheckSession -> checkSession(context)
        }
    }

    private fun googleLogin(idToken: String, context: Context?) {
        if (context == null || idToken.isBlank()) return
        viewModelScope.launch {
            loading()
            runCatching { repo.loginWithGoogle(context, idToken) }
                .onSuccess { _uiState.update { it.copy(isLoading = false, isAuthenticated = true, error = null, isChecking = false) } }
                .onFailure { ex -> _uiState.update { it.copy(isLoading = false, error = ex.message, isChecking = false) } }
        }
    }

    /*
     * loading state
     */
    private fun loading() { _uiState.update { it.copy(isLoading = true, error = null) } }

    /*
     * checks if a backend session already exists
     */
    private fun checkSession(context: Context?) {
        if (context == null) {
            _uiState.update { it.copy(isChecking = false) }
            return
        }
        viewModelScope.launch {
            runCatching { repo.hasActiveSession(context) }
                .onSuccess { has -> _uiState.update { it.copy(isAuthenticated = if (has) true else it.isAuthenticated, isChecking = false) } }
                .onFailure { _uiState.update { it.copy(isChecking = false) } }
        }
    }
}
