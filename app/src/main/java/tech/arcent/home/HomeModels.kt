package tech.arcent.home

/* UI layer achievement model includes optional rich (not poor :)) fields (details, categories) for details screen */

data class Achievement(
    val id: String,
    val title: String,
    val achievedAt: Long,
    val tags: List<String>,
    val photoUrl: String?,
    val details: String? = null,
    val categories: List<String> = emptyList(),
)

sealed interface AllListItem {
    data class DateHeader(val dayStartMillis: Long) : AllListItem

    data class Entry(val achievement: Achievement) : AllListItem
}

data class HomeUiState(
    val achievements: List<Achievement> = emptyList(),
    val allAchievements: List<Achievement> = emptyList(),
    val allListItems: List<AllListItem> = emptyList(),
    val nextCursor: String? = null,
    val loadingMore: Boolean = false,
    val showingAll: Boolean = false,
    val totalWins: Int = achievements.size,
    val streakDays: Int = 0,
    val searching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Achievement> = emptyList(),
    val searchLoading: Boolean = false,
)
