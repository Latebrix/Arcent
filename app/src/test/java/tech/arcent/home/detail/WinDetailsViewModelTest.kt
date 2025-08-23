package tech.arcent.home.detail

/* Unit tests for WinDetailsViewModel covering deletion & duplication flows. */

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import tech.arcent.achievements.data.repo.AchievementCreateRequest
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.home.Achievement
import tech.arcent.testing.MainDispatcherRule
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private class FakeRepo : AchievementRepository {
    var deletedId: String? = null
    var lastAddedPhoto: AchievementPhoto? = null
    private val counter = AtomicInteger(0)
    override suspend fun addAchievement(title: String, details: String?, achievedAt: Long, photo: AchievementPhoto?, categories: List<String>, tags: List<String>): AchievementDomain {
        lastAddedPhoto = photo
        return AchievementDomain("id_${counter.incrementAndGet()}", title, details, achievedAt, photo?.fileName, categories, tags)
    }
    override suspend fun addAchievement(req: AchievementCreateRequest): AchievementDomain {
        lastAddedPhoto = req.photo
        return AchievementDomain("id_${counter.incrementAndGet()}", req.title, req.details, req.achievedAt, req.photo?.fileName, req.categories, req.tags)
    }
    override suspend fun updateAchievement(id: String, title: String, details: String?, achievedAt: Long, photo: AchievementPhoto?, currentPhotoUrl: String?, categories: List<String>, tags: List<String>): AchievementDomain =
        AchievementDomain(id, title, details, achievedAt, currentPhotoUrl, categories, tags)
    override suspend fun deleteAchievement(id: String) { deletedId = id }
    override fun recentFlow(limit: Int) = kotlinx.coroutines.flow.flow<List<AchievementDomain>> { }
    override suspend fun loadPage(cursor: String?, pageSize: Int): AchievementPage = AchievementPage(emptyList(), null)
    override suspend fun search(query: String): List<AchievementDomain> = emptyList()
}

class WinDetailsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test
    fun deletionEmitsAndClearsState() = runTest {
        val repo = FakeRepo()
        val vm = WinDetailsViewModel(repo)
        val ach = Achievement(id = "a1", title = "Test", achievedAt = 1234L, tags = listOf("t"), photoUrl = null, details = "note", categories = listOf("Cat"))
        vm.setAchievement(ach)
        vm.requestDelete()
        advanceUntilIdle()
        val emitted = vm.deleted.first()
        assertEquals("a1", emitted)
        assertEquals("a1", repo.deletedId)
        assertNull(vm.uiState.value.achievement)
        assertFalse(vm.uiState.value.isDeleting)
    }

    @Test
    fun duplicateAddsSuffixAndEmits() = runTest {
        val repo = FakeRepo()
        val vm = WinDetailsViewModel(repo)
        val ach = Achievement(id = "orig", title = "Original Title", achievedAt = 9999L, tags = emptyList(), photoUrl = null, details = null, categories = emptyList())
        vm.setAchievement(ach)
        vm.duplicateCurrent()
        advanceUntilIdle()
        val dup = vm.duplicated.first()
        assertTrue(dup.title.endsWith(" ✦+1"))
        assertEquals(dup, vm.uiState.value.achievement)
        assertFalse(vm.uiState.value.isDuplicating)
    }

    @Test
    fun duplicateCopiesLocalPhoto() = runTest {
        val repo = FakeRepo()
        val vm = WinDetailsViewModel(repo)
        val tmp = File.createTempFile("ach_test", ".jpg").apply { writeBytes(ByteArray(128) { 7 }) }
        val ach = Achievement(id = "p1", title = "Photo", achievedAt = 555L, tags = listOf("x"), photoUrl = "file://${tmp.absolutePath}", details = "d", categories = listOf("C"))
        vm.setAchievement(ach)
        vm.duplicateCurrent()
        advanceUntilIdle()
        vm.duplicated.first()
        assertNotNull(repo.lastAddedPhoto)
        assertTrue(repo.lastAddedPhoto!!.bytes.size >= 128)
    }
}
