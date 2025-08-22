package tech.arcent.achievements.data.remote

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.InputFile
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.arcent.BuildConfig
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.achievements.domain.newLocalAchievement
import tech.arcent.auth.data.AppwriteClientProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/* appwrite implement */
internal class RemoteAchievementRepository(private val context: Context): AchievementRepository {
    private val TAG = "ArcentDebug"
    private fun client(): Client = AppwriteClientProvider.get(context)
    private fun databases(c: Client) = Databases(c)
    private fun storage(c: Client) = Storage(c)
    private val recentFlowInternal = MutableStateFlow<List<AchievementDomain>>(emptyList())
    private var loadedRecent = false
    private var orderSupported: Boolean = true

    override suspend fun addAchievement(title: String, details: String?, achievedAt: Long, photo: AchievementPhoto?, categories: List<String>, tags: List<String>): AchievementDomain {
        val c = client()
        val acc = Account(c).get()
        val userId = acc.id
        var photoUrl: String? = null
        if (photo != null) {
            Log.d(TAG, "Uploading photo: name=${photo.fileName} size=${photo.bytes.size} mime=${photo.mime}")
            val uploaded = storage(c).createFile(
                bucketId = BuildConfig.APPWRITE_BUCKET_ID,
                fileId = ID.unique(),
                file = InputFile.fromBytes(photo.bytes, photo.fileName, photo.mime),
                permissions = listOf(
                    Permission.read(Role.user(userId)),
                    Permission.update(Role.user(userId)),
                    Permission.delete(Role.user(userId))
                )
            )
            photoUrl = "${BuildConfig.appwrite_endpoint}/storage/buckets/${BuildConfig.APPWRITE_BUCKET_ID}/files/${uploaded.id}/view?project=${BuildConfig.appwrite_project_id}"
        }
        val iso = iso8601(achievedAt)
        val domain = newLocalAchievement(title, details, achievedAt, photoUrl, categories, tags)
        databases(c).createDocument(
            databaseId = BuildConfig.APPWRITE_DATABASE_ID,
            collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
            documentId = domain.id,
            data = mapOf(
                "title" to domain.title,
                "details" to (domain.details ?: ""),
                "achievedAt" to iso,
                "photoUrl" to (domain.photoUrl ?: ""),
                "categories" to domain.categories,
                "tags" to domain.tags
            ),
            permissions = listOf(
                Permission.read(Role.user(userId)),
                Permission.update(Role.user(userId)),
                Permission.delete(Role.user(userId))
            )
        )
        recentFlowInternal.value = listOf(domain) + recentFlowInternal.value
        return domain
    }

    override fun recentFlow(limit: Int): Flow<List<AchievementDomain>> {
        if (!loadedRecent) {
            loadedRecent = true
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { loadPage(null, limit) }.onSuccess { recentFlowInternal.value = it.data }
            }
        }
        return recentFlowInternal.asStateFlow()
    }

    override suspend fun loadPage(cursor: String?, pageSize: Int): AchievementPage {
        val c = client()
        fun buildQueries(order: Boolean, currentCursor: String?): MutableList<String> {
            val base = mutableListOf<String>()
            if (order) base += Query.orderDesc("achievedAt")
            base += Query.limit(pageSize)
            if (currentCursor != null) base += Query.cursorAfter(currentCursor)
            return base
        }
        val docs = runCatching { databases(c).listDocuments(
            databaseId = BuildConfig.APPWRITE_DATABASE_ID,
            collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
            queries = buildQueries(orderSupported, cursor)
        ) }.recoverCatching { ex ->
            if (orderSupported && ex is AppwriteException && (ex.message?.contains("Attribute not found", true) == true || ex.message?.contains("achievedAt", true) == true || ex.message?.contains("Invalid query", true) == true)) {
                orderSupported = false
                databases(c).listDocuments(
                    databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                    collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                    queries = buildQueries(false, cursor)
                )
            } else throw ex
        }.getOrThrow()
        val listRaw = docs.documents.map { d ->
            val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
            AchievementDomain(
                id = d.id,
                title = d.data["title"] as? String ?: "",
                details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                achievedAt = epoch,
                photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
        val list = listRaw.sortedByDescending { it.achievedAt }
        val nextCursor = if (docs.documents.size == pageSize) docs.documents.last().id else null
        // Log.d(TAG, "loadPage fetched docs=${docs.documents.size} nextCursor=$nextCursor finalListSize=${list.size}")
        return AchievementPage(list, nextCursor)
    }

    override suspend fun search(query: String): List<AchievementDomain> {
        // Log.d(TAG, "search start query='$query'")
        if (query.isBlank()) return emptyList()
        val c = client()
        val pageSize = 100
        val cap = 800
        val out = LinkedHashMap<String, AchievementDomain>()
        var cursor: String? = null
        var attemptedRemote = false
        do {
            // Log.d(TAG, "search remote page cursor=$cursor orderSupported=$orderSupported")
            val currentCursor = cursor
            fun buildQueries(order: Boolean): MutableList<String> {
                val qs = mutableListOf<String>()
                qs += Query.search("title", query)
                if (orderSupported && order) qs += Query.orderDesc("achievedAt")
                qs += Query.limit(pageSize)
                if (currentCursor != null) qs += Query.cursorAfter(currentCursor)
                return qs
            }
            val docs = runCatching { databases(c).listDocuments(
                databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                queries = buildQueries(orderSupported)
            ) }.recoverCatching { ex ->
                if (orderSupported && ex is AppwriteException && (ex.message?.contains("Attribute not found", true) == true || ex.message?.contains("achievedAt", true) == true || ex.message?.contains("Invalid query", true) == true)) {
                    orderSupported = false
                    databases(c).listDocuments(
                        databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                        collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                        queries = buildQueries(false)
                    )
                } else throw ex
            }.getOrThrow()
            attemptedRemote = true
            docs.documents.forEach { d ->
                val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
                val domain = AchievementDomain(
                    id = d.id,
                    title = d.data["title"] as? String ?: "",
                    details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                    achievedAt = epoch,
                    photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                    categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
                if (out.size < cap) out[domain.id] = domain
            }
            cursor = if (docs.documents.size == pageSize && out.size < cap) docs.documents.last().id else null
        } while (cursor != null)
        if (attemptedRemote && out.isEmpty()) {
            val localOut = LinkedHashMap<String, AchievementDomain>()
            var cursor2: String? = null
            do {
                val currentCursor2 = cursor2
                fun buildQueries2(order: Boolean): MutableList<String> {
                    val qs = mutableListOf<String>()
                    if (orderSupported && order) qs += Query.orderDesc("achievedAt")
                    qs += Query.limit(pageSize)
                    if (currentCursor2 != null) qs += Query.cursorAfter(currentCursor2)
                    return qs
                }
                val docs2 = runCatching { databases(c).listDocuments(
                    databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                    collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                    queries = buildQueries2(orderSupported)
                ) }.recoverCatching { ex ->
                    if (orderSupported && ex is AppwriteException && (ex.message?.contains("Attribute not found", true) == true || ex.message?.contains("achievedAt", true) == true || ex.message?.contains("Invalid query", true) == true)) {
                        orderSupported = false
                        databases(c).listDocuments(
                            databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                            collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                            queries = buildQueries2(false)
                        )
                    } else throw ex
                }.getOrThrow()
                docs2.documents.forEach { d ->
                    val title = (d.data["title"] as? String) ?: ""
                    if (title.contains(query, ignoreCase = true)) {
                        val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
                        val domain = AchievementDomain(
                            id = d.id,
                            title = title,
                            details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                            achievedAt = epoch,
                            photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                            categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        )
                        if (localOut.size < cap) localOut[domain.id] = domain
                    }
                }
                cursor2 = if (docs2.documents.size == pageSize && localOut.size < cap) docs2.documents.last().id else null
            } while (cursor2 != null)
            //Log.d(TAG, "search fallback finished size=${localOut.size}")
            return localOut.values.sortedByDescending { it.achievedAt }
        }
        //Log.d(TAG, "search complete size=${out.size} (no fallback)")
        return out.values.sortedByDescending { it.achievedAt }
    }

    private fun iso8601(epoch: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epoch))
    }
    private fun parseEpoch(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(value)?.time
        }.getOrNull()
        else -> null
    }
}
