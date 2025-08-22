package tech.arcent.achievements.domain

import java.util.UUID

/* Domain model independent from UI layer */
data class AchievementDomain(
    val id: String,
    val title: String,
    val details: String?,
    val achievedAt: Long,
    val photoUrl: String?,
    val categories: List<String>,
    val tags: List<String>
)

/* Page wrapper for pagination */
data class AchievementPage(
    val data: List<AchievementDomain>,
    val nextCursor: String? /* null when end reached */
)

/* Factory helper */
internal fun newLocalAchievement(
    title: String,
    details: String?,
    achievedAt: Long,
    photoUrl: String?,
    categories: List<String>,
    tags: List<String>
): AchievementDomain = AchievementDomain(
    id = UUID.randomUUID().toString(),
    title = title,
    details = details,
    achievedAt = achievedAt,
    photoUrl = photoUrl,
    categories = categories,
    tags = tags
)
