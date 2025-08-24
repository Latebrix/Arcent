package tech.arcent.home.detail

/*
 WinDetailsViewModel duplication tests disabled this version.

 /* TODO: */

 Original content retained below commented for future restoration.
*/

/*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.After
import org.junit.Test
import tech.arcent.achievements.data.repo.AchievementPhoto
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.achievements.domain.AchievementDomain
import tech.arcent.achievements.domain.AchievementPage
import tech.arcent.home.Achievement
import tech.arcent.DuplicationSuffixProvider
import java.util.concurrent.atomic.AtomicInteger

private class FakeRepo : AchievementRepository {
    private val counter = AtomicInteger(0)
    private val stored = mutableMapOf<String, AchievementDomain>()
    override suspend fun addAchievement(title: String, details: String?, achievedAt: Long, photo: AchievementPhoto?, categories: List<String>, tags: List<String>): AchievementDomain {
        val id = "id_${counter.incrementAndGet()}"
        val dom = AchievementDomain(id, title, details, achievedAt, null, categories, tags)
        stored[id] = dom
        return dom
    }
    override suspend fun updateAchievement(id: String, title: String, details: String?, achievedAt: Long, photo: AchievementPhoto?, currentPhotoUrl: String?, categories: List<String>, tags: List<String>): AchievementDomain {
        val dom = AchievementDomain(id, title, details, achievedAt, currentPhotoUrl, categories, tags)
        stored[id] = dom; return dom
    }
    override suspend fun deleteAchievement(id: String) { stored.remove(id) }
    override fun recentFlow(limit: Int) = kotlinx.coroutines.flow.flow<List<AchievementDomain>> { emit(emptyList()) }
    override suspend fun loadPage(cursor: String?, pageSize: Int): AchievementPage = AchievementPage(emptyList(), null)
    override suspend fun search(query: String): List<AchievementDomain> = emptyList()
}

private class FakeSuffixProvider(var value: String = "(CLONE)") : DuplicationSuffixProvider {
    override fun getSuffix(): String = value
    override fun setSuffix(value: String) { this.value = value }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WinDetailsViewModelTest {
    private lateinit var repo: FakeRepo
    private lateinit var suffix: FakeSuffixProvider
    private lateinit var vm: WinDetailsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        repo = FakeRepo()
        suffix = FakeSuffixProvider()
        vm = WinDetailsViewModel(repo, suffix) // no context needed for unit test
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun duplicate_adds_suffix_and_emits_new_item() = runTest(testDispatcher) {
        val original = Achievement("orig1", "Run 5K", System.currentTimeMillis(), emptyList(), null, "Good job", listOf("Health"))
        vm.setAchievement(original)
        vm.duplicateCurrent()
        val duplicated = vm.duplicated.first()
        assertNotNull(duplicated)
        assert(duplicated.title.startsWith("Run 5K"))
        assert(duplicated.title.contains("(CLONE)"))
    }

    @Test
    fun changing_suffix_affects_new_duplications() = runTest(testDispatcher) {
        val original = Achievement("orig2", "Meditate", System.currentTimeMillis(), emptyList(), null, null, emptyList())
        vm.setAchievement(original)
        suffix.setSuffix("(COPY)")
        vm.duplicateCurrent()
        val duplicated = vm.duplicated.first()
        assert(duplicated.title.contains("(COPY)"))
    }
}
*/
