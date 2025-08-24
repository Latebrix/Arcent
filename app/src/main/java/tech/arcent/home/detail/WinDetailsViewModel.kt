package tech.arcent.home.detail

/*
 details vm duplication disabled this version; duplication code commented but preserved.
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.arcent.achievements.data.repo.AchievementCreateRequest
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.crash.CrashReporting
import tech.arcent.crash.safeIo
import tech.arcent.home.Achievement
import java.io.File
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import tech.arcent.auth.data.SessionManager
import tech.arcent.BuildConfig

@HiltViewModel
class WinDetailsViewModel @Inject constructor(
    private val repo: AchievementRepository,
    @ApplicationContext private val appContext: Context? = null,
) : ViewModel() {
    data class UiState(
        val achievement: Achievement? = null,
        val isDeleting: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _deleted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deleted: SharedFlow<String> = _deleted.asSharedFlow()

    fun setAchievement(a: Achievement) { _uiState.value = _uiState.value.copy(achievement = a) }

    fun requestDelete() {
        val id = _uiState.value.achievement?.id ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            safeIo { repo.deleteAchievement(id) }
            _deleted.emit(id)
            _uiState.value = _uiState.value.copy(isDeleting = false, achievement = null)
        }
    }

    /*
    fun duplicateCurrent() { /* disabled */ }
    */

    // photo loading kept (may be reused later)
    private suspend fun loadPhotoForDuplication(photoUrl: String?): AchievementPhoto? {
        if (photoUrl.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                when {
                    photoUrl.startsWith("file://") -> {
                        val path = photoUrl.removePrefix("file://")
                        fileToPhoto(File(path))
                    }
                    photoUrl.startsWith("/") -> fileToPhoto(File(photoUrl))
                    photoUrl.startsWith("http") -> remoteUrlToPhoto(photoUrl)
                    else -> null
                }
            }.onFailure { CrashReporting.capture(it) }.getOrNull()
        }
    }

    private fun fileToPhoto(file: File): AchievementPhoto? {
        if (!file.exists()) return null
        val bytes = runCatching { file.readBytes() }.getOrElse { return null }
        val ext = file.extension.lowercase()
        val mime = when (ext) { "png" -> "image/png"; "webp" -> "image/webp"; "jpg", "jpeg" -> "image/jpeg"; else -> "image/jpeg" }
        return AchievementPhoto(bytes, mime, "dup_${System.currentTimeMillis()}.$ext")
    }

    private fun remoteUrlToPhoto(url: String): AchievementPhoto? {
        val client = OkHttpClient()
        val cleanPath = url.substringBefore('?')
        val lastSegment = cleanPath.substringAfterLast('/')
        val extCandidate = lastSegment.substringAfterLast('.', "").lowercase()
        val ext = when (extCandidate) { "png", "jpg", "jpeg", "webp" -> extCandidate; else -> "jpg" }
        val mime = when (ext) { "png" -> "image/png"; "webp" -> "image/webp"; "jpg", "jpeg" -> "image/jpeg"; else -> "image/jpeg" }
        val secret = appContext?.let { SessionManager.load(it)?.second }
        val endpoint = BuildConfig.appwrite_endpoint
        val reqBuilder = Request.Builder().url(url)
        if (!secret.isNullOrBlank() && !endpoint.isNullOrBlank() && url.contains(endpoint)) {
            reqBuilder.addHeader("X-Appwrite-Project", BuildConfig.appwrite_project_id)
            reqBuilder.addHeader("X-Appwrite-Session", secret)
        }
        return client.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) { CrashReporting.breadcrumb("duplicate_photo", "download_fail_code=${resp.code}"); return null }
            val body = resp.body?.bytes() ?: return null
            AchievementPhoto(body, mime, "dup_${System.currentTimeMillis()}.$ext")
        }
    }
}
