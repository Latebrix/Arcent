package tech.arcent.auth

/*
 ViewModel managing authentication state and session checks with injected repository (Hilt). now wrapped with crash reporting helpers so exceptions dont crash ui
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import tech.arcent.auth.data.AuthRepository
import tech.arcent.crash.CrashReporting
import tech.arcent.crash.safeLaunch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isChecking: Boolean = true,
    val localMode: Boolean = false,
    val localName: String = "",
    val localNameError: String? = null,
)

sealed class AuthEvent {
    data class GoogleToken(val idToken: String) : AuthEvent()

    data class GoogleFailed(val statusCode: Int, val msg: String?) : AuthEvent()

    object LocalRequested : AuthEvent()

    data class LocalNameChanged(val value: String) : AuthEvent()

    object LocalSubmit : AuthEvent()

    object CheckSession : AuthEvent()
}

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val repo: AuthRepository,
        @ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        fun onEvent(
            e: AuthEvent,
            context: Context? = null,
        ) {
            try {
                when (e) {
                    is AuthEvent.GoogleToken -> googleLogin(e.idToken, context ?: appContext)
                    is AuthEvent.GoogleFailed -> _uiState.update { it.copy(error = "google_${e.statusCode}:${e.msg}", isLoading = false) }
                    AuthEvent.LocalRequested -> _uiState.update { it.copy(localMode = true, error = null) }
                    is AuthEvent.LocalNameChanged -> _uiState.update { it.copy(localName = e.value, localNameError = null) }
                    AuthEvent.LocalSubmit -> submitLocal(context ?: appContext)
                    AuthEvent.CheckSession -> checkSession(context ?: appContext)
                }
            } catch (ex: Exception) {
                CrashReporting.capture(ex)
            }
        }

        private fun submitLocal(context: Context) {
            val name = _uiState.value.localName.trim()
            if (name.length < 2) {
                _uiState.update { it.copy(localNameError = "Name too short") }
                return
            }
            viewModelScope.safeLaunch {
                try {
                    runCatching { tech.arcent.auth.data.LocalUserStore.saveLocalUserName(context, name) }
                        .onSuccess { _uiState.update { it.copy(isAuthenticated = true, isLoading = false) } }
                        .onFailure { ex ->
                            CrashReporting.capture(ex)
                            _uiState.update { it.copy(localNameError = ex.message ?: "Failed to save") }
                        }
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
            }
        }

        private fun googleLogin(
            idToken: String,
            context: Context,
        ) {
            if (idToken.isBlank()) return
            viewModelScope.safeLaunch {
                try {
                    loading()
                    runCatching { repo.loginWithGoogle(context, idToken) }
                        .onSuccess {
                            runCatching { repo.fetchAndCacheProfile(context) }
                                .onFailure { CrashReporting.capture(it) }
                            _uiState.update { it.copy(isLoading = false, isAuthenticated = true, error = null, isChecking = false) }
                        }
                        .onFailure { ex ->
                            CrashReporting.capture(ex)
                            _uiState.update { it.copy(isLoading = false, error = ex.message, isChecking = false) }
                        }
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
            }
        }

        // Indicates loading state
        private fun loading() {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        // Checks for existing remote or local session
        private fun checkSession(context: Context) {
            viewModelScope.safeLaunch {
                try {
                    val remoteHas =
                        runCatching { repo.hasActiveSession(context) }
                            .onFailure { CrashReporting.capture(it) }
                            .getOrElse { false }
                    if (remoteHas) runCatching { repo.fetchAndCacheProfile(context) }.onFailure { CrashReporting.capture(it) }
                    val localProfile =
                        runCatching { tech.arcent.auth.data.UserProfileStore.load(context) }
                            .onFailure { CrashReporting.capture(it) }
                            .getOrNull()
                    val localHas = (localProfile?.provider == "local" && !localProfile.name.isNullOrBlank())
                    _uiState.update { it.copy(isAuthenticated = remoteHas || localHas || it.isAuthenticated, isChecking = false) }
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
            }
        }
    }
