package tech.arcent.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tech.arcent.auth.data.AuthRepository
import tech.arcent.auth.data.AppwriteAuthRepository
import tech.arcent.auth.data.LocalUserStore
import tech.arcent.auth.data.UserProfileStore

/*
 * Model for managing authentication
 */

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isChecking: Boolean = true,
    val localMode: Boolean = false,
    val localName: String = "",
    val localNameError: String? = null
)

sealed class AuthEvent {
    data class GoogleToken(val idToken: String): AuthEvent()
    data class GoogleFailed(val statusCode: Int, val msg: String?): AuthEvent()
    object LocalRequested: AuthEvent()
    data class LocalNameChanged(val value: String): AuthEvent()
    object LocalSubmit: AuthEvent()
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
            AuthEvent.LocalRequested -> _uiState.update { it.copy(localMode = true, error = null) }
            is AuthEvent.LocalNameChanged -> _uiState.update { it.copy(localName = e.value, localNameError = null) }
            AuthEvent.LocalSubmit -> submitLocal(context)
            AuthEvent.CheckSession -> checkSession(context)
        }
    }

    private fun submitLocal(context: Context?) {
        if (context == null) return
        val name = _uiState.value.localName.trim()
        if (name.length < 2) {
            _uiState.update { it.copy(localNameError = "Name too short") }
            return
        }
        viewModelScope.launch {
            runCatching { LocalUserStore.saveLocalUserName(context, name) }
                .onSuccess { _uiState.update { it.copy(isAuthenticated = true, isLoading = false) } }
                .onFailure { ex -> _uiState.update { it.copy(localNameError = ex.message ?: "Failed to save") } }
        }
    }

    private fun googleLogin(idToken: String, context: Context?) {
        if (context == null || idToken.isBlank()) return
        viewModelScope.launch {
            loading()
            runCatching { repo.loginWithGoogle(context, idToken) }
                .onSuccess {
                    // fetch profile one time
                    runCatching { repo.fetchAndCacheProfile(context) }
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, error = null, isChecking = false) }
                }
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
            // First: remote google session check
            val remoteHas = runCatching { repo.hasActiveSession(context) }.getOrElse { false }
            if (remoteHas) runCatching { repo.fetchAndCacheProfile(context) }
            // Then: local profile check (provider == local)
            val localProfile = UserProfileStore.load(context)
            val localHas = (localProfile?.provider == "local" && !localProfile.name.isNullOrBlank())
            _uiState.update { it.copy(isAuthenticated = remoteHas || localHas || it.isAuthenticated, isChecking = false) }
        }
    }
}
