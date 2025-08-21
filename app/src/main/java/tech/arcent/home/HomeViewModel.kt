package tech.arcent.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(achievements = sampleAchievements()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /* Add a newly created achievement to the top of the list */
    fun addAchievement(a: Achievement) {
        _uiState.value = _uiState.value.copy(achievements = listOf(a) + _uiState.value.achievements)
    }
}
