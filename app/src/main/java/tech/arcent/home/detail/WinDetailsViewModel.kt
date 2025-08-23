package tech.arcent.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class WinDetailsViewModel @Inject constructor(private val repo: AchievementRepository) : ViewModel() {
    data class UiState(
        val achievement: Achievement? = null,
        val isDeleting: Boolean = false,
        val isDuplicating: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _deleted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deleted: SharedFlow<String> = _deleted.asSharedFlow()

    private val _duplicated = MutableSharedFlow<Achievement>(extraBufferCapacity = 1)
    val duplicated: SharedFlow<Achievement> = _duplicated.asSharedFlow()

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

    fun duplicateCurrent() {
        val a = _uiState.value.achievement ?: return
        if (_uiState.value.isDuplicating) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDuplicating = true)
            val suffixTitle = a.title + " (✦ +1)"
            val photo: AchievementPhoto? = loadPhotoForDuplication(a.photoUrl)
            val domain = safeIo {
                repo.addAchievement(
                    AchievementCreateRequest(
                        title = suffixTitle,
                        details = a.details,
                        achievedAt = a.achievedAt,
                        photo = photo,
                        categories = a.categories,
                        tags = a.tags,
                    ),
                )
            }
            if (domain != null) {
                val newAch = Achievement(
                    id = domain.id,
                    title = domain.title,
                    achievedAt = domain.achievedAt,
                    tags = domain.tags,
                    photoUrl = domain.photoUrl,
                    details = domain.details,
                    categories = domain.categories,
                )
                _duplicated.emit(newAch)
                _uiState.value = _uiState.value.copy(isDuplicating = false, achievement = newAch)
            } else {
                _uiState.value = _uiState.value.copy(isDuplicating = false)
            }
        }
    }

    private suspend fun loadPhotoForDuplication(photoUrl: String?): AchievementPhoto? {
        if (photoUrl.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                if (photoUrl.startsWith("file://")) {
                    val path = photoUrl.removePrefix("file://")
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val mime = when (path.substringAfterLast('.', "").lowercase()) {
                            "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg"
                        }
                        AchievementPhoto(bytes, mime, "dup_${file.name}")
                    } else null
                } else if (photoUrl.startsWith("http")) {
                    val url = URL(photoUrl)
                    url.openStream().use { ins ->
                        val bytes = ins.readBytes()
                        val ext = photoUrl.substringAfterLast('.', "").lowercase()
                        val mime = when (ext) { "png" -> "image/png"; "webp" -> "image/webp"; "jpg", "jpeg" -> "image/jpeg"; else -> "image/jpeg" }
                        AchievementPhoto(bytes, mime, "dup_${System.currentTimeMillis()}.$ext")
                    }
                } else null
            }.onFailure { CrashReporting.capture(it) }.getOrNull()
        }
    }
}
