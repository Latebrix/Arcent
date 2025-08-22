package tech.arcent.repo

/*
* instrumentation variant of dispatcher usage test: ensures addAchievement dispatches via custom dispatcher
*/

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.arcent.achievements.data.local.LocalAchievementRepository

private class CountingDispatcher2 : CoroutineDispatcher() {
    private val base = StandardTestDispatcher()
    var dispatchCount = 0

    override fun dispatch(
        context: kotlin.coroutines.CoroutineContext,
        block: Runnable,
    ) {
        dispatchCount++
        base.dispatch(context, block)
    }
}

@RunWith(AndroidJUnit4::class)
class LocalRepoDispatcherUsageInstrumentedTest {
    @Test
    fun addUsesDispatcher() =
        runTest {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val counting = CountingDispatcher2()
            val repo = LocalAchievementRepository(ctx, counting)
            repo.addAchievement("dTitle", "dDetails", System.currentTimeMillis(), null, emptyList(), emptyList())
            assertTrue(counting.dispatchCount > 0)
        }
}
