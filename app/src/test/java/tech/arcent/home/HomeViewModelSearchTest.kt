package tech.arcent.home

/* Unit tests for HomeViewModel search debounce behavior without relying on StandardTestDispatcher (uses small real delay). */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.data.repo.AchievementCreateRequest
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.core.dispatchers.AppDispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeRepo : AchievementRepository {
    private val list = mutableListOf<AchievementDomain>()
    val searchCalls = mutableListOf<String>()
    override suspend fun addAchievement(title: String, details: String?, achievedAt: Long, photo: tech.arcent.achievements.data.repo.AchievementPhoto?, categories: List<String>, tags: List<String>): AchievementDomain = throw UnsupportedOperationException()
    override suspend fun addAchievement(req: AchievementCreateRequest): AchievementDomain = throw UnsupportedOperationException()
    override suspend fun updateAchievement(id: String, title: String, details: String?, achievedAt: Long, photo: tech.arcent.achievements.data.repo.AchievementPhoto?, currentPhotoUrl: String?, categories: List<String>, tags: List<String>): AchievementDomain = throw UnsupportedOperationException()
    override suspend fun deleteAchievement(id: String) {}
    override fun recentFlow(limit: Int) = kotlinx.coroutines.flow.flow<List<AchievementDomain>> { emit(list) }
    override suspend fun loadPage(cursor: String?, pageSize: Int): AchievementPage = AchievementPage(emptyList(), null)
    override suspend fun search(query: String): List<AchievementDomain> {
        searchCalls += query
        return listOf(AchievementDomain("id-$query", query, null, 0L, null, emptyList(), emptyList()))
    }
}

private class UnconfinedDispatchers : AppDispatchers {
    override val main = Dispatchers.Unconfined
    override val io = Dispatchers.Unconfined
}

class HomeViewModelSearchTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:org.junit.Rule
    val mainRule = object : TestWatcher() {
        override fun starting(description: Description) { Dispatchers.setMain(Dispatchers.Unconfined) }
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun finished(description: Description) { Dispatchers.resetMain() }
    }

    @Before
    fun setup() { HomeViewModel.OVERRIDE_DEBOUNCE_MS = 40L }

    private suspend fun waitForSearch(repo: FakeRepo) {
        repeat(50) {
            if (repo.searchCalls.isNotEmpty()) return
            yield()
        }
    }

    @Test
    fun debounce_executes_only_after_pause() = runBlocking {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, UnconfinedDispatchers())
        vm.onSearchQueryChange("E")
        vm.onSearchQueryChange("Eg")
        vm.onSearchQueryChange("Egypt")
        delay(90) /* exceed debounce */
        yield()
        assertEquals(listOf("Egypt"), repo.searchCalls)
        val results = vm.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Egypt", results.first().title)
    }

    @Test
    fun blank_query_clears_immediately() = runBlocking {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, UnconfinedDispatchers())
        vm.onSearchQueryChange("Test")
        delay(90)
        yield()
        assertEquals(listOf("Test"), repo.searchCalls)
        assertEquals(1, vm.uiState.value.searchResults.size)
        vm.onSearchQueryChange("")
        yield()
        assertTrue(vm.uiState.value.searchResults.isEmpty())
        assertEquals(listOf("Test"), repo.searchCalls)
    }
}
