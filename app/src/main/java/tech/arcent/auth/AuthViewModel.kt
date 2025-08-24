package tech.arcent.auth

/*
 ViewModel managing authentication state and session checks with injected repository (Hilt). now wrapped with crash reporting helpers so exceptions dont crash ui
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import tech.arcent.BuildConfig
import tech.arcent.auth.data.AuthRepository
import tech.arcent.auth.data.LocalUserStore
import tech.arcent.auth.data.SessionManager
import tech.arcent.auth.data.UserProfileStore
import tech.arcent.crash.safeLaunch
import tech.arcent.session.SessionEvents
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
                tech.arcent.crash.CrashReporting.capture(ex)
            }
        }

        /* logout clearing all cached user/session artifacts then updating state so root shows auth screen */
        fun logout(context: Context) {
            viewModelScope.safeLaunch {
                try {
                    runCatching {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(BuildConfig.google_server_client_id).requestEmail().build()
                        GoogleSignIn.getClient(context, gso).signOut()
                    }
                    runCatching { SessionManager.clear(context) }
                    runCatching { UserProfileStore.clear(context) }
                    runCatching { LocalUserStore.clearLocalUser(context) }
                    tech.arcent.di.AchievementRepoCacheResetHolder.reset()
                } catch (e: Exception) {
                    tech.arcent.crash.CrashReporting.capture(e)
                } finally {
                    _uiState.update { it.copy(isAuthenticated = false, isChecking = false, isLoading = false) }
                    SessionEvents.fireAuthChanged()
                }
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
                        .onSuccess {
                            // persist profile so settings loads immediately without restart
                            runCatching { UserProfileStore.saveProfile(context, name = name, avatarUrl = null, provider = "local") }
                            _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                            SessionEvents.fireAuthChanged()
                        }
                        .onFailure { ex ->
                            tech.arcent.crash.CrashReporting.capture(ex)
                            _uiState.update { it.copy(localNameError = ex.message ?: "Failed to save") }
                        }
                } catch (e: Exception) {
                    tech.arcent.crash.CrashReporting.capture(e)
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
                            runCatching { repo.fetchAndCacheProfile(context) }.onFailure { tech.arcent.crash.CrashReporting.capture(it) }
                            tech.arcent.di.AchievementRepoCacheResetHolder.reset()
                            _uiState.update { it.copy(isLoading = false, isAuthenticated = true, error = null, isChecking = false) }
                            SessionEvents.fireAuthChanged()
                        }
                        .onFailure { ex ->
                            tech.arcent.crash.CrashReporting.capture(ex)
                            _uiState.update { it.copy(isLoading = false, error = ex.message, isChecking = false) }
                        }
                } catch (e: Exception) {
                    tech.arcent.crash.CrashReporting.capture(e)
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
                            .onFailure { tech.arcent.crash.CrashReporting.capture(it) }
                            .getOrElse { false }
                    if (remoteHas) runCatching { repo.fetchAndCacheProfile(context) }.onFailure { tech.arcent.crash.CrashReporting.capture(it) }
                    val localProfile =
                        runCatching { tech.arcent.auth.data.UserProfileStore.load(context) }
                            .onFailure { tech.arcent.crash.CrashReporting.capture(it) }
                            .getOrNull()
                    val localHas = (localProfile?.provider == "local" && !localProfile.name.isNullOrBlank())
                    _uiState.update { it.copy(isAuthenticated = remoteHas || localHas || it.isAuthenticated, isChecking = false) }
                } catch (e: Exception) {
                    tech.arcent.crash.CrashReporting.capture(e)
                }
            }
        }
    }
