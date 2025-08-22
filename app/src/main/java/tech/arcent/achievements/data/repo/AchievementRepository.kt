package tech.arcent.achievements.data.repo

import kotlinx.coroutines.flow.Flow
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage

/*
 Batched creation request to reduce long parameter lists
 */
data class AchievementCreateRequest(
    val title: String,
    val details: String?,
    val achievedAt: Long,
    val photo: AchievementPhoto?,
    val categories: List<String>,
    val tags: List<String>,
)

data class AchievementPhoto(val bytes: ByteArray, val mime: String, val fileName: String)

interface AchievementRepository {
    suspend fun addAchievement(
        title: String,
        details: String?,
        achievedAt: Long,
        photo: AchievementPhoto?,
        categories: List<String>,
        tags: List<String>,
    ): AchievementDomain

    /*
     Overload using request object for cleaner call sites
     */
    suspend fun addAchievement(req: AchievementCreateRequest): AchievementDomain =
        addAchievement(req.title, req.details, req.achievedAt, req.photo, req.categories, req.tags)

    fun recentFlow(limit: Int): Flow<List<AchievementDomain>>

    suspend fun loadPage(
        cursor: String?,
        pageSize: Int,
    ): AchievementPage

    /*
     Search across all data (remote auto-pages under a safety cap)
     */
    suspend fun search(query: String): List<AchievementDomain>
}
