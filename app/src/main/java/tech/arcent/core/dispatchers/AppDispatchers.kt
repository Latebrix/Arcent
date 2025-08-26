package tech.arcent.core.dispatchers

/* Abstraction layer for coroutine dispatchers to allow deterministic testing and centralized control. */

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
}

private class DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers()
}

