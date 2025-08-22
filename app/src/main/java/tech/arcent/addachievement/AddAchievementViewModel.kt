package tech.arcent.addachievement

/*
 ViewModel for adding achievements: now injected via Hilt for testability.
 */

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tech.arcent.achievements.data.repo.AchievementCreateRequest
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.home.Achievement
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AddAchievementViewModel
    @Inject
    constructor(
        private val repo: AchievementRepository,
        @ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddAchievementUiState())
        val uiState: StateFlow<AddAchievementUiState> = _uiState.asStateFlow()

        private val _saved = MutableSharedFlow<Achievement>(extraBufferCapacity = 1)
        val saved: SharedFlow<Achievement> = _saved.asSharedFlow()

        fun onEvent(ev: AddAchievementEvent) {
            when (ev) {
                is AddAchievementEvent.TitleChanged -> _uiState.update { it.copy(title = ev.value.take(175)) }
                is AddAchievementEvent.DetailsChanged -> _uiState.update { it.copy(details = ev.value.take(4750)) }
                is AddAchievementEvent.CategorySelected ->
                    _uiState.update { st ->
                        val newSel = if (st.selectedCategory == ev.category) null else ev.category
                        st.copy(selectedCategory = newSel)
                    }
                is AddAchievementEvent.TagToggled ->
                    _uiState.update {
                        val next = it.selectedTags.toMutableSet()
                        if (!next.add(ev.tag)) next.remove(ev.tag)
                        it.copy(selectedTags = next)
                    }
                is AddAchievementEvent.DateChanged -> _uiState.update { it.copy(dateMillis = ev.millis, showDatePicker = false) }
                is AddAchievementEvent.TimeChanged ->
                    _uiState.update {
                        it.copy(
                            hour = ev.hour,
                            minute = ev.minute,
                            showTimePicker = false,
                        )
                    }
                is AddAchievementEvent.ImagePicked -> _uiState.update { it.copy(imageUri = ev.uri) }
                AddAchievementEvent.ToggleDatePicker -> _uiState.update { it.copy(showDatePicker = !it.showDatePicker) }
                AddAchievementEvent.ToggleTimePicker -> _uiState.update { it.copy(showTimePicker = !it.showTimePicker) }
                AddAchievementEvent.ToggleTips -> _uiState.update { it.copy(showTipsSheet = !it.showTipsSheet) }
                AddAchievementEvent.Save -> save()
                is AddAchievementEvent.AddCategory ->
                    _uiState.update { st ->
                        if (ev.name.isBlank()) {
                            st
                        } else {
                            st.copy(
                                userCategories = (st.userCategories + ev.name).distinct(),
                                showAddCategoryDialog = false,
                            )
                        }
                    }
                is AddAchievementEvent.AddTag ->
                    _uiState.update { st ->
                        if (ev.name.isBlank()) {
                            st
                        } else {
                            st.copy(
                                userTags = (st.userTags + ev.name).distinct(),
                                showAddTagDialog = false,
                            )
                        }
                    }
                AddAchievementEvent.ToggleAddCategoryDialog ->
                    _uiState.update {
                        it.copy(
                            showAddCategoryDialog = !it.showAddCategoryDialog,
                        )
                    }
                AddAchievementEvent.ToggleAddTagDialog -> _uiState.update { it.copy(showAddTagDialog = !it.showAddTagDialog) }
                AddAchievementEvent.Clear ->
                    _uiState.update {
                        AddAchievementUiState(
                            userCategories = it.userCategories,
                            userTags = it.userTags,
                        )
                    }
            }
        }

        private fun save() {
            val s = _uiState.value
            if (s.title.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val cal =
                    Calendar.getInstance().apply {
                        timeInMillis = s.dateMillis
                        set(Calendar.HOUR_OF_DAY, s.hour)
                        set(Calendar.MINUTE, s.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                val achievedAt = cal.timeInMillis
                val localPhoto = s.imageUri?.toString()
                val photo = s.imageUri?.let { uri -> readPhoto(uri)?.let { (bytes, mime, name) -> AchievementPhoto(bytes, mime, name) } }
                val req =
                    AchievementCreateRequest(
                        title = s.title.trim(),
                        details = s.details.ifBlank { null },
                        achievedAt = achievedAt,
                        photo = photo,
                        categories = listOfNotNull(s.selectedCategory),
                        tags = s.selectedTags.toList(),
                    )
                val domain = repo.addAchievement(req)
                _saved.emit(
                    Achievement(
                        id = domain.id,
                        title = domain.title,
                        achievedAt = domain.achievedAt,
                        tags = domain.tags,
                        photoUrl = localPhoto ?: domain.photoUrl,
                    ),
                )
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private fun readPhoto(uri: android.net.Uri): Triple<ByteArray, String, String>? {
            return runCatching {
                val cr: ContentResolver = appContext.contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val name = uri.lastPathSegment ?: "photo" + System.currentTimeMillis()
                cr.openInputStream(uri)?.use { ins -> ins.readBytes() }?.let { Triple(it, mime, name) }
            }.getOrNull()
        }
    }

private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
    this.value = block(this.value)
}
