package tech.arcent.repo

/*
 instrumentation test for local repo basic add + page + search (uses real Room db)
 */

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.arcent.achievements.data.local.LocalAchievementRepository

@RunWith(AndroidJUnit4::class)
class LocalAchievementRepositoryAndroidTest {
    // simple insertion then page
    @Test
    fun addThenLoadPageAndSearch() =
        runTest {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val repo = LocalAchievementRepository(ctx, StandardTestDispatcher(testScheduler))
            val now = System.currentTimeMillis()
            val a = repo.addAchievement("Test Title", "Some details", now, null, emptyList(), listOf("tag1"))
            assertEquals("Test Title", a.title)
            val page = repo.loadPage(null, 10)
            assertTrue(page.data.isNotEmpty())
            val search = repo.search("Test")
            assertTrue(search.any { it.title == "Test Title" })
        }
}
