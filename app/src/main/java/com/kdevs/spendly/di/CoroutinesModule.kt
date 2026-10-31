package com.kdevs.spendly.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Dispatchers are injected (not hard-coded) so tests can swap them for a test dispatcher. */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
