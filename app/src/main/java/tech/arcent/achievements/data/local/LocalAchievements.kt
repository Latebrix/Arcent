package tech.arcent.achievements.data.local

/*
 local repo backed by Room plus wipe helpers
 */

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.achievements.domain.newLocalAchievement
import tech.arcent.crash.CrashReporting
import java.io.File

// Room entity achievements
@Entity(tableName = "achievements")
internal data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val details: String?,
    val achievedAt: Long,
    val photoUrl: String?,
    val categoriesCsv: String?,
    val tagsCsv: String?,
)

// CSV helpesr
private fun List<String>.toCsv(): String? = if (isEmpty()) null else joinToString(",")

private fun String?.csvToList(): List<String> = this?.takeIf { it.isNotBlank() }?.split(',') ?: emptyList()

@Dao
internal interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: AchievementEntity)

    @Query("DELETE FROM achievements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM achievements ORDER BY achievedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY achievedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun page(
        limit: Int,
        offset: Int,
    ): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun count(): Int

    @Query(
        "SELECT * FROM achievements WHERE title LIKE '%' || :q || '%' OR details LIKE '%' || :q || '%' ORDER BY achievedAt DESC LIMIT :limit",
    )
    suspend fun searchAll(
        q: String,
        limit: Int,
    ): List<AchievementEntity>

    @Query("DELETE FROM achievements")
    suspend fun clearAll()
}

@Database(entities = [AchievementEntity::class], version = 1, exportSchema = false)
internal abstract class AchievementsDb : RoomDatabase() {
    abstract fun dao(): AchievementDao

    companion object {
        @Volatile private var inst: AchievementsDb? = null

        fun get(ctx: Context): AchievementsDb =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(ctx.applicationContext, AchievementsDb::class.java, "achievements.db").build().also { inst = it }
            }
    }
}

// local repos implementation
internal class LocalAchievementRepository(private val context: Context, private val io: CoroutineDispatcher) : AchievementRepository {
    private val dao = AchievementsDb.get(context).dao()
    private val pageSizeInternal = 20
    private val searchCap = 400 // safety cap

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
                val photoPath =
                    photo?.let {
                        try {
                            val dir = File(context.filesDir, "achievements_photos").apply { if (!exists()) mkdirs() }
                            val ext =
                                when {
                                    it.mime.contains("png", true) -> "png"
                                    it.mime.contains("webp", true) -> "webp"
                                    else -> "jpg"
                                }
                            val file = File(dir, "${System.currentTimeMillis()}_${title.hashCode()}.$ext")
                            runCatching { file.writeBytes(it.bytes) }
                            if (file.exists()) file.absolutePath else null
                        } catch (e: Exception) {
                            CrashReporting.capture(e)
                            null
                        }
                    }
                val domain = newLocalAchievement(title, details, achievedAt, photoPath?.let { "file://" + it }, categories, tags)
                val entity =
                    AchievementEntity(
                        domain.id,
                        domain.title,
                        domain.details,
                        domain.achievedAt,
                        domain.photoUrl,
                        domain.categories.toCsv(),
                        domain.tags.toCsv(),
                    )
                try {
                    dao.insert(entity)
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
                domain
            } catch (e: Exception) {
                CrashReporting.capture(e)
                newLocalAchievement("local_error", details, System.currentTimeMillis(), null, emptyList(), emptyList())
            }
        }

    override suspend fun updateAchievement(
        id: String,
        title: String,
        details: String?,
        achievedAt: Long,
        photo: AchievementPhoto?,
        currentPhotoUrl: String?,
        categories: List<String>,
        tags: List<String>,
    ): AchievementDomain =
        withContext(io) {
            try {
                val newPhoto =
                    photo?.let {
                        try {
                            val dir = File(context.filesDir, "achievements_photos").apply { if (!exists()) mkdirs() }
                            val ext =
                                when {
                                    it.mime.contains("png", true) -> "png"
                                    it.mime.contains("webp", true) -> "webp"
                                    else -> "jpg"
                                }
                            val file = File(dir, "${System.currentTimeMillis()}_${title.hashCode()}.$ext")
                            runCatching { file.writeBytes(it.bytes) }
                            if (file.exists()) file.absolutePath else null
                        } catch (e: Exception) {
                            CrashReporting.capture(e)
                            null
                        }
                    }
                val finalPhotoUrl = newPhoto?.let { "file://" + it } ?: currentPhotoUrl
                val domain = AchievementDomain(id, title, details, achievedAt, finalPhotoUrl, categories, tags)
                val entity = AchievementEntity(id, title, details, achievedAt, finalPhotoUrl, categories.toCsv(), tags.toCsv())
                try { dao.insert(entity) } catch (e: Exception) { CrashReporting.capture(e) }
                domain
            } catch (e: Exception) {
                CrashReporting.capture(e)
                AchievementDomain(id, title, details, achievedAt, currentPhotoUrl, categories, tags)
            }
        }

    override suspend fun deleteAchievement(id: String) {
        withContext(io) {
            try { dao.delete(id) } catch (e: Exception) { CrashReporting.capture(e) }
        }
    }

    override fun recentFlow(limit: Int): Flow<List<AchievementDomain>> =
        dao.recent(limit).map { list ->
            try {
                list.map { it.toDomain() }
            } catch (e: Exception) {
                CrashReporting.capture(e)
                emptyList()
            }
        }

    private var lastOffset = 0

    override suspend fun loadPage(
        cursor: String?,
        pageSize: Int,
    ): AchievementPage =
        withContext(io) {
            try {
                val size = if (pageSize <= 0) pageSizeInternal else pageSize
                val offset = cursor?.toIntOrNull() ?: lastOffset
                val data =
                    try {
                        dao.page(size, offset).map { it.toDomain() }
                    } catch (e: Exception) {
                        CrashReporting.capture(e)
                        emptyList()
                    }
                lastOffset = offset + data.size
                val total =
                    try {
                        dao.count()
                    } catch (e: Exception) {
                        CrashReporting.capture(e)
                        0
                    }
                val next = if (lastOffset >= total || data.isEmpty()) null else lastOffset.toString()
                AchievementPage(data, next)
            } catch (e: Exception) {
                CrashReporting.capture(e)
                AchievementPage(emptyList(), null)
            }
        }

    override suspend fun search(query: String): List<AchievementDomain> =
        withContext(io) {
            if (query.isBlank()) return@withContext emptyList()
            try {
                dao.searchAll(query, searchCap).map { it.toDomain() }
            } catch (e: Exception) {
                CrashReporting.capture(e)
                emptyList()
            }
        }
}

// mapping
private fun AchievementEntity.toDomain(): AchievementDomain =
    AchievementDomain(
        id,
        title,
        details,
        achievedAt,
        photoUrl,
        categoriesCsv.csvToList(),
        tagsCsv.csvToList(),
    )

suspend fun wipeLocalAchievements(context: Context) {
    runCatching { AchievementsDb.get(context).dao().clearAll() }.onFailure { CrashReporting.capture(it) }
    runCatching { File(context.filesDir, "achievements_photos").deleteRecursively() }.onFailure { CrashReporting.capture(it) }
}
