package tech.arcent.home

/*
 ViewModel for home screen: manages recent wins, paging, search,  detail updates/deletes.
*/

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.data.repo.toUi
import tech.arcent.core.dispatchers.AppDispatchers
import tech.arcent.session.SessionEvents
import java.util.Calendar
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repo: AchievementRepository,
        private val dispatchers: AppDispatchers,
    ) : ViewModel() {
        companion object {
            internal var OVERRIDE_DEBOUNCE_MS: Long? = null
        }
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private val recentLimit = 5
        private val searchQueryFlow = MutableStateFlow("")
        private val searchDebounceMs = 350L

        init {
            viewModelScope.launch {
                repo.recentFlow(recentLimit).collect { list ->
                    val ui = list.map { it.toUi() }
                    _uiState.value = _uiState.value.copy(achievements = ui, totalWins = ui.size)
                }
            }
            loadMoreInternal(reset = true)
            viewModelScope.launch {
                SessionEvents.authChanges.collect {
                    _uiState.value = HomeUiState()
                    loadMoreInternal(reset = true)
                }
            }
            viewModelScope.launch {
                searchQueryFlow
                    .debounce(OVERRIDE_DEBOUNCE_MS ?: searchDebounceMs)
                    .distinctUntilChanged()
                    .collectLatest { qRaw ->
                        val q = qRaw.trim()
                        if (q.isBlank()) {
                            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false)
                            return@collectLatest
                        }
                        _uiState.value = _uiState.value.copy(searchLoading = true)
                        /* avoid starting first heavy search while initial paging still loading to prevent first-run lag */
                        if (_uiState.value.loadingMore) {
                            uiState.filter { !it.loadingMore }.first()
                        }
                        val results = try {
                            withContext(dispatchers.io) { repo.search(q).map { it.toUi() } }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Exception) {
                            emptyList()
                        }
                        _uiState.value = _uiState.value.copy(searchResults = results, searchLoading = false)
                    }
            }
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
            val cal =
                Calendar.getInstance().apply {
                    timeInMillis = millis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            return cal.timeInMillis
        }

        /* Search control functions */
        fun openSearch() { _uiState.value = _uiState.value.copy(searching = true, searchQuery = "", searchResults = emptyList(), searchLoading = false); searchQueryFlow.value = "" }

        fun closeSearch() { _uiState.value = _uiState.value.copy(searching = false, searchQuery = "", searchResults = emptyList(), searchLoading = false); searchQueryFlow.value = "" }

        fun onSearchQueryChange(q: String) { _uiState.value = _uiState.value.copy(searchQuery = q); if (q.isBlank()) { _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false) }; searchQueryFlow.value = q }

        fun onNewAchievement(a: Achievement) {
            val current = _uiState.value
            val existingIdx = current.allAchievements.indexOfFirst { it.id == a.id }
            val baseList = current.allAchievements.toMutableList()
            if (existingIdx >= 0) {
                baseList[existingIdx] = a
            } else {
                baseList.add(0, a)
            }
            val sorted = baseList.sortedWith(compareByDescending<Achievement> { it.achievedAt }.thenByDescending { if (it.id == a.id) 1 else 0 })
            val grouped = buildAllListItems(sorted)
            val recents = current.achievements.toMutableList().let { list ->
                val recIdx = list.indexOfFirst { it.id == a.id }
                if (recIdx >= 0) list[recIdx] = a else list.add(0, a)
                list.take(recentLimit)
            }
            val searchResults = current.searchResults.map { if (it.id == a.id) a else it }
            _uiState.value = current.copy(allAchievements = sorted, allListItems = grouped, achievements = recents, searchResults = searchResults)
        }

        fun onDeleteAchievement(id: String) {
            val current = _uiState.value
            val all = current.allAchievements.filterNot { it.id == id }
            val grouped = buildAllListItems(all)
            val recents = current.achievements.filterNot { it.id == id }
            val search = current.searchResults.filterNot { it.id == id }
            _uiState.value = current.copy(allAchievements = all, allListItems = grouped, achievements = recents, searchResults = search)
        }

        fun deleteAchievement(id: String) {
            viewModelScope.launch { runCatching { repo.deleteAchievement(id) }; onDeleteAchievement(id) }
        }
    }
