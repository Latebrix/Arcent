package tech.arcent.repo

/*
 instrumentation test: toggles profile provider and asserts dynamic repo picks correct backend (local/remote). simplistic but covers switch logic
 */

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.arcent.auth.data.UserProfileStore
import tech.arcent.di.DynamicAchievementRepository

@RunWith(AndroidJUnit4::class)
class DynamicAchievementRepositoryAndroidTest {
    @Test
    fun providerSwitchLocalThenRemote() =
        runTest {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            UserProfileStore.saveProfile(ctx, name = "Ali", avatarUrl = null, provider = "local")
            val repo = DynamicAchievementRepository(ctx, StandardTestDispatcher(testScheduler))
            assertEquals("local", repo._activeProviderName())
            UserProfileStore.saveProfile(ctx, name = "Ali", avatarUrl = null, provider = "google")
            assertEquals("remote", repo._activeProviderName())
        }
}
