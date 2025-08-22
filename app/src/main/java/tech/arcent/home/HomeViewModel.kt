package tech.arcent.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.arcent.achievements.data.repo.AchievementRepositoryProvider
import tech.arcent.achievements.data.repo.toUi
import java.util.Calendar

/* ViewModel handles recent list, paginated All list, and simple full search (internally auto-paged in repo) */
class HomeViewModel(application: Application): AndroidViewModel(application) {
    private val repo = AchievementRepositoryProvider.get(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val recentLimit = 5
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repo.recentFlow(recentLimit).collect { list ->
                val ui = list.map { it.toUi() }
                _uiState.value = _uiState.value.copy(achievements = ui, totalWins = ui.size)
            }
        }
        loadMoreInternal(reset = true)
    }

    fun toggleAll() { _uiState.value = _uiState.value.copy(showingAll = !_uiState.value.showingAll) }

    fun loadMore() { if (!_uiState.value.loadingMore && _uiState.value.nextCursor != null) loadMoreInternal() }

    private fun loadMoreInternal(reset: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMore = true)
            val cursor = if (reset) null else _uiState.value.nextCursor
            val page = repo.loadPage(cursor, 20)
            val uiNew = page.data.map { it.toUi() }
            val merged = if (reset) uiNew else _uiState.value.allAchievements + uiNew
            val grouped = buildAllListItems(merged)
            _uiState.value = _uiState.value.copy(allAchievements = merged, allListItems = grouped, nextCursor = page.nextCursor, loadingMore = false)
        }
    }

    private fun buildAllListItems(list: List<Achievement>): List<AllListItem> {
        if (list.isEmpty()) return emptyList()
        val byDay = list.groupBy { dayStart(it.achievedAt) }.toSortedMap(compareByDescending { it })
        val out = mutableListOf<AllListItem>()
        byDay.forEach { (day, achievements) ->
            out += AllListItem.DateHeader(day)
            achievements.sortedByDescending { it.achievedAt }.forEach { out += AllListItem.Entry(it) }
        }
        return out
    }

    private fun dayStart(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        return cal.timeInMillis
    }

    /* Search */
    fun openSearch() { _uiState.value = _uiState.value.copy(searching = true, searchQuery = "", searchResults = emptyList(), searchLoading = false) }
    fun closeSearch() { _uiState.value = _uiState.value.copy(searching = false, searchQuery = "", searchResults = emptyList(), searchLoading = false) }
    fun onSearchQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
        triggerSearchDebounced()
    }

    private fun triggerSearchDebounced() {
        searchJob?.cancel()
        val snapshot = _uiState.value.searchQuery
        searchJob = viewModelScope.launch {
            val q = snapshot.trim()
            if (q.isBlank()) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false)
                return@launch
            }
            _uiState.value = _uiState.value.copy(searchLoading = true)
            delay(300)
            val results = runCatching { repo.search(q).map { it.toUi() } }.getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(searchResults = results, searchLoading = false)
        }
    }

    fun onNewAchievement(a: Achievement) {
        val all = listOf(a) + _uiState.value.allAchievements
        val grouped = buildAllListItems(all)
        _uiState.value = _uiState.value.copy(allAchievements = all, allListItems = grouped)
    }
}
