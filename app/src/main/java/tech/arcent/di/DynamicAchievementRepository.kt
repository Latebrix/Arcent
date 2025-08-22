package tech.arcent.di

/*
 dynamic repo chooser; picks local or remote each call. not super fancy but flexible if profile flips.
 */

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import tech.arcent.achievements.data.local.LocalAchievementRepository
import tech.arcent.achievements.data.remote.RemoteAchievementRepository
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.auth.data.UserProfileStore

internal class DynamicAchievementRepository(
    private val context: Context,
    private val io: CoroutineDispatcher,
) : AchievementRepository {
    private val local by lazy { LocalAchievementRepository(context, io) }
    private val remote by lazy { RemoteAchievementRepository(context, io) }

    private fun active(): AchievementRepository {
        val profile = UserProfileStore.load(context)
        return if (profile?.provider == "local") local else remote
    }

    internal fun _activeProviderName(): String = if ((UserProfileStore.load(context)?.provider) == "local") "local" else "remote"

    override suspend fun addAchievement(
        title: String,
        details: String?,
        achievedAt: Long,
        photo: AchievementPhoto?,
        categories: List<String>,
        tags: List<String>,
    ) = active().addAchievement(
        title,
        details,
        achievedAt,
        photo,
        categories,
        tags,
    )

    override fun recentFlow(limit: Int) = active().recentFlow(limit)

    override suspend fun loadPage(
        cursor: String?,
        pageSize: Int,
    ) = active().loadPage(cursor, pageSize)

    override suspend fun search(query: String) = active().search(query)
}
