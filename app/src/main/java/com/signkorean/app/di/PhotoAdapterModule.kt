package com.signkorean.app.di

import com.signkorean.app.learn.CoilPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotoAdapterModule {
    @Provides
    @Singleton
    fun provideLearnPhotoPrefetcher(prefetcher: CoilPhotoPrefetcher): LearnPhotoPrefetcher = prefetcher
}
