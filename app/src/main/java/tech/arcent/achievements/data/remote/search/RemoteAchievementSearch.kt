package tech.arcent.achievements.data.remote

/*
 remote search logic.
 */

import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import tech.arcent.BuildConfig
import tech.arcent.achievements.data.remote.util.parseEpoch
import tech.arcent.achievements.domain.AchievementDomain

internal class RemoteAchievementSearch(
    private val clientProvider: () -> Client,
    private val databasesProvider: (Client) -> Databases,
) {
    private var orderSupported: Boolean = true
    private val pageSize = 100
    private val cap = 800

    suspend fun search(query: String): List<AchievementDomain> {
        if (query.isBlank()) return emptyList()
        val c = clientProvider()
        val out = LinkedHashMap<String, AchievementDomain>()
        var cursor: String? = null
        var attemptedRemote = false
        do {
            val currentCursor = cursor
            val docs = listDocuments(c, buildPrimaryQueries(currentCursor, query))
            attemptedRemote = true
            docs.documents.forEach { d ->
                val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
                val domain =
                    AchievementDomain(
                        id = d.id,
                        title = d.data["title"] as? String ?: "",
                        details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                        achievedAt = epoch,
                        photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                        categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    )
                if (out.size < cap) out[domain.id] = domain
            }
            cursor = if (docs.documents.size == pageSize && out.size < cap) docs.documents.last().id else null
        } while (cursor != null)
        if (attemptedRemote && out.isEmpty()) return fallbackScan(c, query)
        return out.values.sortedByDescending { it.achievedAt }
    }

    private suspend fun listDocuments(
        c: Client,
        queries: List<String>,
    ) = kotlin.runCatching {
        databasesProvider(c).listDocuments(
            databaseId = BuildConfig.APPWRITE_DATABASE_ID,
            collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
            queries = queries,
        )
    }.recoverCatching { ex ->
        if (orderSupported && ex is AppwriteException && (ex.message?.contains("Attribute not found", true) == true || ex.message?.contains("achievedAt", true) == true || ex.message?.contains("Invalid query", true) == true)) {
            orderSupported = false
            databasesProvider(c).listDocuments(
                databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                queries = queries.filterNot { it.startsWith("order") },
            )
        } else {
            throw ex
        }
    }.getOrThrow()

    private fun buildPrimaryQueries(
        cursor: String?,
        query: String,
    ): List<String> {
        val qs = mutableListOf<String>()
        qs += Query.search("title", query)
        if (orderSupported) qs += Query.orderDesc("achievedAt")
        qs += Query.limit(pageSize)
        if (cursor != null) qs += Query.cursorAfter(cursor)
        return qs
    }

    private suspend fun fallbackScan(
        c: Client,
        query: String,
    ): List<AchievementDomain> {
        val localOut = LinkedHashMap<String, AchievementDomain>()
        var cursor: String? = null
        do {
            val docs = listDocuments(c, buildFallbackQueries(cursor))
            docs.documents.forEach { d ->
                val title = (d.data["title"] as? String) ?: ""
                if (title.contains(query, true)) {
                    val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
                    val domain =
                        AchievementDomain(
                            id = d.id,
                            title = title,
                            details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                            achievedAt = epoch,
                            photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                            categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        )
                    if (localOut.size < cap) localOut[domain.id] = domain
                }
            }
            cursor = if (docs.documents.size == pageSize && localOut.size < cap) docs.documents.last().id else null
        } while (cursor != null)
        return localOut.values.sortedByDescending { it.achievedAt }
    }

    private fun buildFallbackQueries(cursor: String?): List<String> {
        val qs = mutableListOf<String>()
        if (orderSupported) qs += Query.orderDesc("achievedAt")
        qs += Query.limit(pageSize)
        if (cursor != null) qs += Query.cursorAfter(cursor)
        return qs
    }
}
