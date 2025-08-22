package tech.arcent.achievements.data.repo

import kotlinx.coroutines.flow.Flow
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage

data class AchievementPhoto(val bytes: ByteArray, val mime: String, val fileName: String)

interface AchievementRepository {
    suspend fun addAchievement(
        title: String,
        details: String?,
        achievedAt: Long,
        photo: AchievementPhoto?,
        categories: List<String>,
        tags: List<String>
    ): AchievementDomain
    fun recentFlow(limit: Int): Flow<List<AchievementDomain>>
    suspend fun loadPage(cursor: String?, pageSize: Int): AchievementPage
    /* Search across all data (remote auto-pages under a safety cap) */
    suspend fun search(query: String): List<AchievementDomain>
}
