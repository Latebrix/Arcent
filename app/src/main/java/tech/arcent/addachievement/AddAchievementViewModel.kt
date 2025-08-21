package tech.arcent.addachievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.arcent.home.Achievement
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class AddAchievementViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(AddAchievementUiState())
    val uiState: StateFlow<AddAchievementUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<Achievement>(extraBufferCapacity = 1)
    val saved: SharedFlow<Achievement> = _saved.asSharedFlow()

    fun onEvent(ev: AddAchievementEvent) {
        when (ev) {
            is AddAchievementEvent.TitleChanged -> _uiState.update { it.copy(title = ev.value.take(175)) }
            is AddAchievementEvent.DetailsChanged -> _uiState.update { it.copy(details = ev.value.take(4750)) }
            is AddAchievementEvent.CategorySelected -> _uiState.update { st ->
                val newSel = if (st.selectedCategory == ev.category) null else ev.category
                st.copy(selectedCategory = newSel)
            }
            is AddAchievementEvent.TagToggled -> _uiState.update {
                val next = it.selectedTags.toMutableSet()
                if (!next.add(ev.tag)) next.remove(ev.tag)
                it.copy(selectedTags = next)
            }
            is AddAchievementEvent.DateChanged -> _uiState.update { it.copy(dateMillis = ev.millis, showDatePicker = false) }
            is AddAchievementEvent.TimeChanged -> _uiState.update { it.copy(hour = ev.hour, minute = ev.minute, showTimePicker = false) }
            is AddAchievementEvent.ImagePicked -> _uiState.update { it.copy(imageUri = ev.uri) }
            AddAchievementEvent.ToggleDatePicker -> _uiState.update { it.copy(showDatePicker = !it.showDatePicker) }
            AddAchievementEvent.ToggleTimePicker -> _uiState.update { it.copy(showTimePicker = !it.showTimePicker) }
            AddAchievementEvent.ToggleTips -> _uiState.update { it.copy(showTipsSheet = !it.showTipsSheet) }
            AddAchievementEvent.Save -> save()
            is AddAchievementEvent.AddCategory -> _uiState.update { st ->
                if (ev.name.isBlank()) st else st.copy(
                    userCategories = (st.userCategories + ev.name).distinct(),
                    showAddCategoryDialog = false
                )
            }
            is AddAchievementEvent.AddTag -> _uiState.update { st ->
                if (ev.name.isBlank()) st else st.copy(
                    userTags = (st.userTags + ev.name).distinct(),
                    showAddTagDialog = false
                )
            }
            AddAchievementEvent.ToggleAddCategoryDialog -> _uiState.update { it.copy(showAddCategoryDialog = !it.showAddCategoryDialog) }
            AddAchievementEvent.ToggleAddTagDialog -> _uiState.update { it.copy(showAddTagDialog = !it.showAddTagDialog) }
            AddAchievementEvent.Clear -> _uiState.update { AddAchievementUiState(userCategories = it.userCategories, userTags = it.userTags) }
        }
    }

    private fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val dateStr = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(s.dateMillis))
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", s.hour, s.minute)
            val timestamp = "$dateStr • $timeStr"
            val achievement = Achievement(
                title = s.title.trim(),
                timestamp = timestamp,
                tags = s.selectedTags.toList(),
                imageRes = tech.arcent.R.drawable.ic_splash
            )
            _saved.emit(achievement)
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}

/* Simple extension to update MutableStateFlow */
private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) { this.value = block(this.value) }
