package tech.arcent.settings

/*
 ViewModel handling settings actions: crash reporting toggle, avatar change, logout/delete (remote only), name change.
*/

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import io.appwrite.services.Storage
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.arcent.BuildConfig
import tech.arcent.achievements.data.local.wipeLocalAchievements
import tech.arcent.auth.AuthViewModel
import tech.arcent.auth.data.LocalUserStore
import tech.arcent.auth.data.SessionManager
import tech.arcent.auth.data.UserProfileStore
import tech.arcent.auth.data.UserProfile
import tech.arcent.auth.data.AppwriteClientProvider
import tech.arcent.crash.CrashPrefs
import tech.arcent.crash.CrashReporting
import tech.arcent.crash.CrashRuntimeInit
import tech.arcent.profile.ProfileEvents
import java.io.File
import java.io.InputStream
import java.net.SocketTimeoutException
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import tech.arcent.session.SessionEvents

data class SettingsUiState(
    val crashEnabled: Boolean = true,
    val isDebug: Boolean = BuildConfig.DEBUG,
    val versionName: String = BuildConfig.VERSION_NAME,
    val deleting: Boolean = false,
    val loggingOut: Boolean = false,
    val avatarPath: String? = null,
    val avatarUploading: Boolean = false,
    val provider: String = "",
    val nameInput: String = "",
    val savedName: String = "",
    val nameError: String? = null,
    val savingName: Boolean = false,
)

sealed class SettingsEvent {
    object NameSaved : SettingsEvent()
    object AvatarSaved : SettingsEvent()
    data class Error(val resKey: String) : SettingsEvent()
}

/* previous cloud response parser retained for reference
internal object AvatarFunctionResponseParser {
    private val fileIdRegex = Regex("\"fileId\"\\s*:\\s*\"([^\\"]+)\"")
    fun parseFileId(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = fileIdRegex.find(body)
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
*/

/* previous cloud upload implementation retained (disabled)
private suspend fun uploadRemoteAvatarViaFunction(bytes: ByteArray, mime: String): String? = withContext(Dispatchers.IO) {
    try {
        val client = AppwriteClientProvider.get(appContext)
        val account = runCatching { Account(client).get() }.getOrNull() ?: return@withContext null
        val functionId = BuildConfig.APPWRITE_PROFILE_IMAGES_FUN
        if (functionId.isBlank()) return@withContext null
        val bucketId = BuildConfig.APPWRITE_PROFILE_IMAGES_BUCKET
        if (bucketId.isBlank()) return@withContext null
        val endpoint = BuildConfig.appwrite_endpoint?.trimEnd('/') ?: return@withContext null
        val projectId = BuildConfig.appwrite_project_id
        if (projectId.isBlank()) return@withContext null
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val payload = "{" + "\"file\":\"" + base64 + "\",\"userId\":\"" + account.id + "\"}"
        val execution = runCatching {
            Functions(client).createExecution(
                functionId = functionId,
                body = payload,
                method = "POST",
                headers = mapOf("Content-Type" to "application/json"),
            )
        }.onFailure { if (it is SocketTimeoutException) CrashReporting.nonFatal("avatar_function_timeout"); CrashReporting.capture(it) }.getOrNull() ?: return@withContext null
        if (execution.status != "completed") return@withContext null
        val body = execution.responseBody ?: return@withContext null
        val fileId = AvatarFunctionResponseParser.parseFileId(body) ?: return@withContext null
        val remoteUrl = "$endpoint/storage/buckets/$bucketId/files/$fileId/view?project=$projectId"
        remoteUrl
    } catch (e: Exception) {
        if (e is SocketTimeoutException) CrashReporting.nonFatal("avatar_function_timeout_catch")
        CrashReporting.capture(e); null
    }
}
*/

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val profile: UserProfile? get() = UserProfileStore.load(appContext)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            crashEnabled = CrashPrefs.isEnabled(appContext, BuildConfig.DEBUG),
            avatarPath = UserProfileStore.load(appContext)?.avatarPath,
            provider = UserProfileStore.load(appContext)?.provider ?: "",
            nameInput = UserProfileStore.load(appContext)?.name ?: "",
            savedName = UserProfileStore.load(appContext)?.name ?: "",
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val maxNameLen = 75
    private val maxAvatarBytes = 45L * 1024 * 1024

    init {
        viewModelScope.launch { SessionEvents.authChanges.collect { refreshFromProfile() } }
    }

    private fun refreshFromProfile() {
        val p = UserProfileStore.load(appContext)
        _uiState.update {
            it.copy(
                avatarPath = p?.avatarPath,
                provider = p?.provider ?: "",
                nameInput = p?.name ?: "",
                savedName = p?.name ?: "",
            )
        }
    }

    fun toggleCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { CrashPrefs.optIn(appContext, enabled) }
            if (enabled) {
                CrashReporting.enable(); CrashRuntimeInit.ensureInitialized(appContext)
            } else CrashReporting.disable()
            _uiState.update { it.copy(crashEnabled = enabled) }
        }
    }

    fun onNameInput(value: String) {
        _uiState.update { it.copy(nameInput = value.take(maxNameLen), nameError = null) }
    }

    fun saveName(authVm: AuthViewModel) {
        val newName = _uiState.value.nameInput.trim()
        if (newName.length < 2) {
            _uiState.update { it.copy(nameError = "short") }
            return
        }
        if (newName.length > maxNameLen) {
            _uiState.update { it.copy(nameError = "long") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(savingName = true, nameError = null) }
            var success = false
            try {
                val prov = profile?.provider
                if (prov == "local") {
                    runCatching { LocalUserStore.saveLocalUserName(appContext, newName) }.onFailure { CrashReporting.capture(it) }.onSuccess { success = true }
                } else if (prov == "google") {
                    withContext(Dispatchers.IO) {
                        runCatching { Account(AppwriteClientProvider.get(appContext)).updateName(newName) }.onFailure { CrashReporting.capture(it) }.onSuccess { success = true }
                    }
                    if (success) UserProfileStore.saveProfile(appContext, name = newName, avatarUrl = profile?.avatarPath, provider = prov ?: "google")
                }
                if (success) {
                    ProfileEvents.refresh(appContext)
                    _events.tryEmit(SettingsEvent.NameSaved)
                } else {
                    _events.tryEmit(SettingsEvent.Error("name_save"))
                }
            } catch (e: Exception) {
                CrashReporting.capture(e)
                _uiState.update { it.copy(nameError = e.message ?: "err") }
                _events.tryEmit(SettingsEvent.Error("name_save"))
            } finally {
                _uiState.update { cur ->
                    cur.copy(
                        savingName = false,
                        savedName = if (success) newName else cur.savedName,
                    )
                }
            }
        }
    }

    fun setAvatar(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(avatarUploading = true) }
            try {
                val saved = saveAvatarInternal(uri)
                if (saved == null) {
                    _events.tryEmit(SettingsEvent.Error("avatar_upload"))
                    return@launch
                }
                val prov = profile?.provider ?: "local"
                UserProfileStore.saveProfile(appContext, profile?.name, saved, prov)
                ProfileEvents.refresh(appContext)
                _uiState.update { it.copy(avatarPath = saved) }
                _events.tryEmit(SettingsEvent.AvatarSaved)
            } finally {
                _uiState.update { it.copy(avatarUploading = false) }
            }
        }
    }

    private fun saveAvatarBytesLocally(context: Context, bytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            CrashReporting.capture(e)
            null
        }
    }

    private suspend fun saveAvatarInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val input: InputStream? = appContext.contentResolver.openInputStream(uri)
            if (input == null) return@withContext null
            val bytes = input.readBytes()
            if (bytes.size.toLong() > maxAvatarBytes) {
                _events.tryEmit(SettingsEvent.Error("avatar_size"))
                return@withContext null
            }
            /* regardless of provider (local or google) we now persist only locally */
            saveAvatarBytesLocally(appContext, bytes)
        } catch (e: Exception) {
            if (e is AppwriteException) {
                CrashReporting.breadcrumb("avatar", "appwrite_error stage=save_internal code=${e.code} type=${e.type}")
                CrashReporting.nonFatal("avatar_internal_appwrite_exception_code_${e.code}")
            }
            CrashReporting.capture(e); null
        }
    }

    private fun resetUiAfterAccountChange() {
        _uiState.update { it.copy(nameInput = "", savedName = "", avatarPath = null, provider = "") }
    }

    fun logoutAndReturn(authVm: AuthViewModel, onDone: () -> Unit) {
        if (profile?.provider == "local") { onDone(); return }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loggingOut = true) }
                runCatching { revokeRemoteSession() }
                runCatching { SessionManager.clear(appContext) }
                // remove locally cached avatars on logout
                runCatching { File(appContext.filesDir, "avatars").deleteRecursively() }
                authVm.logout(appContext)
                resetUiAfterAccountChange()
            } finally {
                _uiState.update { it.copy(loggingOut = false) }
                onDone()
            }
        }
    }

    private suspend fun revokeRemoteSession() = withContext(Dispatchers.IO) {
        try { Account(AppwriteClientProvider.get(appContext)).deleteSession("current") } catch (_: Exception) {}
    }

    fun deleteAndReturn(authVm: AuthViewModel, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(deleting = true) }
                val prov = profile?.provider
                if (prov == "google") {
                    withContext(Dispatchers.IO) { runCatching { deleteRemoteAchievementsAndAccount() }.onFailure { CrashReporting.capture(it) } }
                }
                runCatching { LocalUserStore.clearLocalUser(appContext) }
                runCatching { UserProfileStore.clear(appContext) }
                runCatching { wipeLocalAchievements(appContext) }
                File(appContext.filesDir, "avatars").deleteRecursively()
                runCatching { SessionManager.clear(appContext) }
                authVm.logout(appContext)
                resetUiAfterAccountChange()
            } finally {
                _uiState.update { it.copy(deleting = false) }
                onDone()
            }
        }
    }

    private fun extractFileId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val marker = "/files/"
        val idx = url.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val end = url.indexOf('/', start)
        if (end == -1) return null
        return url.substring(start, end)
    }
    /* paginated deletion loop, removes achievements and associated photos before deleting account */
    private suspend fun deleteRemoteAchievementsAndAccount() {
        val client = AppwriteClientProvider.get(appContext)
        val account = runCatching { Account(client).get() }.getOrNull() ?: return
        val databases = Databases(client)
        val storage = Storage(client)
        val dbId = BuildConfig.APPWRITE_DATABASE_ID
        val colId = BuildConfig.APPWRITE_COLLECTION_ID
        if (dbId.isBlank() || colId.isBlank()) return
        // paginated deletion loop
        while (true) {
            val docs = runCatching { databases.listDocuments(databaseId = dbId, collectionId = colId, queries = listOf(io.appwrite.Query.limit(25))) }.getOrNull() ?: break
            if (docs.documents.isEmpty()) break
            docs.documents.forEach { d ->
                // attempt delete associated photo
                val photoUrl = (d.data["photoUrl"] as? String)?.takeIf { it.isNotBlank() }
                val fileId = extractFileId(photoUrl)
                if (!fileId.isNullOrBlank() && BuildConfig.APPWRITE_BUCKET_ID.isNotBlank()) {
                    runCatching { storage.deleteFile(bucketId = BuildConfig.APPWRITE_BUCKET_ID, fileId = fileId) }
                }
                runCatching { databases.deleteDocument(databaseId = dbId, collectionId = colId, documentId = d.id) }
            }
        }
        // delete profile avatar file if stored
        runCatching { deleteAccountHttp() }
        runCatching { Account(client).deleteSession("current") }
    }

    private suspend fun deleteAccountHttp() = withContext(Dispatchers.IO) {
        try {
            val secret = SessionManager.load(appContext)?.second ?: return@withContext
            val endpoint = BuildConfig.appwrite_endpoint?.trimEnd('/') ?: return@withContext
            val projectId = BuildConfig.appwrite_project_id
            if (projectId.isBlank()) return@withContext
            val url = endpoint + "/account"
            val http = OkHttpClient()
            val req = Request.Builder()
                .url(url)
                .delete()
                .addHeader("X-Appwrite-Project", projectId)
                .addHeader("X-Appwrite-Session", secret)
                .build()
            http.newCall(req).execute().use { /* ignore body */ }
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }
}
