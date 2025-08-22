package tech.arcent.achievements.data.remote

/*
 remote repo using appwrite
 */

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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.arcent.BuildConfig
import tech.arcent.achievements.data.remote.util.parseEpoch
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.achievements.domain.newLocalAchievement
import tech.arcent.auth.data.AppwriteClientProvider
import tech.arcent.crash.CrashReporting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class RemoteAchievementRepository(private val context: Context, private val io: CoroutineDispatcher) : AchievementRepository {
    private val TAG = "ArcentDebug"

    private fun client(): Client = AppwriteClientProvider.get(context)

    private fun databases(c: Client) = Databases(c)

    private fun storage(c: Client) = Storage(c)

    private val recentFlowInternal = MutableStateFlow<List<AchievementDomain>>(emptyList())
    private var loadedRecent = false
    private val searchService = RemoteAchievementSearch(::client, ::databases)
    private val scope = CoroutineScope(io + SupervisorJob())

    // add achievement remote with photo upload
    override suspend fun addAchievement(
        title: String,
        details: String?,
        achievedAt: Long,
        photo: AchievementPhoto?,
        categories: List<String>,
        tags: List<String>,
    ): AchievementDomain =
        withContext(io) {
            try {
                val c = client()
                val acc = Account(c).get()
                val userId = acc.id
                var photoUrl: String? = null
                if (photo != null) {
                    Log.d(TAG, "Uploading photo: name=${photo.fileName} size=${photo.bytes.size} mime=${photo.mime}")
                    val uploaded =
                        storage(c).createFile(
                            bucketId = BuildConfig.APPWRITE_BUCKET_ID,
                            fileId = ID.unique(),
                            file = InputFile.fromBytes(photo.bytes, photo.fileName, photo.mime),
                            permissions =
                                listOf(
                                    Permission.read(Role.user(userId)),
                                    Permission.update(Role.user(userId)),
                                    Permission.delete(Role.user(userId)),
                                ),
                        )
                    photoUrl = "${BuildConfig.appwrite_endpoint}/storage/buckets/${BuildConfig.APPWRITE_BUCKET_ID}/files/${uploaded.id}/view?project=${BuildConfig.appwrite_project_id}"
                }
                val iso = iso8601(achievedAt)
                val domain = newLocalAchievement(title, details, achievedAt, photoUrl, categories, tags)
                databases(c).createDocument(
                    databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                    collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                    documentId = domain.id,
                    data =
                        mapOf(
                            "title" to domain.title,
                            "details" to (domain.details ?: ""),
                            "achievedAt" to iso,
                            "photoUrl" to (domain.photoUrl ?: ""),
                            "categories" to domain.categories,
                            "tags" to domain.tags,
                        ),
                    permissions =
                        listOf(
                            Permission.read(Role.user(userId)),
                            Permission.update(Role.user(userId)),
                            Permission.delete(Role.user(userId)),
                        ),
                )
                recentFlowInternal.value = listOf(domain) + recentFlowInternal.value
                domain
            } catch (e: Exception) {
                CrashReporting.capture(e)
                newLocalAchievement("remote_error", details, System.currentTimeMillis(), null, emptyList(), emptyList())
            }
        }

    // recent flow lazy load first page safe
    override fun recentFlow(limit: Int): Flow<List<AchievementDomain>> {
        if (!loadedRecent) {
            loadedRecent = true
            scope.launch {
                try {
                    val page = loadPage(null, limit)
                    recentFlowInternal.value = page.data
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
            }
        }
        return recentFlowInternal.asStateFlow()
    }

    // paged load safe with fallback empty page if fail
    override suspend fun loadPage(
        cursor: String?,
        pageSize: Int,
    ): AchievementPage =
        withContext(io) {
            try {
                val c = client()
                var orderSupported = true

                fun buildQueries(
                    order: Boolean,
                    currentCursor: String?,
                ): MutableList<String> {
                    val base = mutableListOf<String>()
                    if (order) base += Query.orderDesc("achievedAt")
                    base += Query.limit(pageSize)
                    if (currentCursor != null) base += Query.cursorAfter(currentCursor)
                    return base
                }
                val docs =
                    runCatching {
                        databases(c).listDocuments(
                            databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                            collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                            queries = buildQueries(orderSupported, cursor),
                        )
                    }.recoverCatching { ex ->
                        if (orderSupported && ex is AppwriteException && (ex.message?.contains("Attribute not found", true) == true || ex.message?.contains("achievedAt", true) == true || ex.message?.contains("Invalid query", true) == true)) {
                            orderSupported = false
                            databases(c).listDocuments(
                                databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                                collectionId = BuildConfig.APPWRITE_COLLECTION_ID,
                                queries = buildQueries(false, cursor),
                            )
                        } else {
                            throw ex
                        }
                    }.getOrThrow()
                val listRaw =
                    docs.documents.map { d ->
                        val epoch = parseEpoch(d.data["achievedAt"]) ?: System.currentTimeMillis()
                        AchievementDomain(
                            id = d.id,
                            title = d.data["title"] as? String ?: "",
                            details = (d.data["details"] as? String).takeUnless { it.isNullOrBlank() },
                            achievedAt = epoch,
                            photoUrl = (d.data["photoUrl"] as? String).takeUnless { it.isNullOrBlank() },
                            categories = (d.data["categories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            tags = (d.data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        )
                    }
                val list = listRaw.sortedByDescending { it.achievedAt }
                val nextCursor = if (docs.documents.size == pageSize) docs.documents.last().id else null
                AchievementPage(list, nextCursor)
            } catch (e: Exception) {
                CrashReporting.capture(e)
                AchievementPage(emptyList(), null)
            }
        }

    // remote search safe returns empty when fails
    override suspend fun search(query: String): List<AchievementDomain> =
        withContext(io) {
            try {
                searchService.search(query)
            } catch (e: Exception) {
                CrashReporting.capture(e)
                emptyList()
            }
        }

    private fun iso8601(epoch: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epoch))
    }
}
