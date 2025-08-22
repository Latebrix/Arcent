package tech.arcent.di

/*
 modules for wiring repos + auth.
 */

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import tech.arcent.achievements.data.repo.AchievementRepository
import tech.arcent.auth.data.AppwriteAuthRepository
import tech.arcent.auth.data.AuthRepository
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModules {
    @Provides @Singleton
    fun provideAuthRepository(): AuthRepository = AppwriteAuthRepository()

    @Provides @Singleton @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun provideAchievementRepository(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
    ): AchievementRepository = DynamicAchievementRepository(context, io)
}
