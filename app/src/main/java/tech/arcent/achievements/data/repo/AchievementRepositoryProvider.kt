package tech.arcent.achievements.data.repo

import android.content.Context
import tech.arcent.achievements.data.local.LocalAchievementRepository
import tech.arcent.achievements.data.remote.RemoteAchievementRepository
import tech.arcent.auth.data.UserProfileStore

/* provider deciding Room or Appwrite */
object AchievementRepositoryProvider {
    @Volatile private var repo: AchievementRepository? = null
    fun get(context: Context): AchievementRepository {
        val existing = repo
        if (existing != null) return existing
        return synchronized(this) {
            val profile = UserProfileStore.load(context)
            val created = if (profile?.provider == "local") LocalAchievementRepository(context) else RemoteAchievementRepository(context)
            repo = created
            created
        }
    }
}

/* helpers for mapping domain to UI layer */
fun tech.arcent.achievements.domain.AchievementDomain.toUi(): tech.arcent.home.Achievement = tech.arcent.home.Achievement(
    id = id,
    title = title,
    achievedAt = achievedAt,
    tags = tags,
    photoUrl = photoUrl
)
