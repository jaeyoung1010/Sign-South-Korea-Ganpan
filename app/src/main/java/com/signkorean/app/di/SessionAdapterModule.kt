package com.signkorean.app.di

import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.data.repository.RoomSessionCreationRepository
import com.signkorean.data.repository.UuidSessionIdGenerator
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionCreationRepository
import com.signkorean.domain.port.output.SessionIdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionAdapterModule {
    @Provides
    @Singleton
    fun provideSessionCreationRepository(
        database: SignKoreanDatabase,
        clock: ClockPort,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): SessionCreationRepository = RoomSessionCreationRepository(database, clock, dispatcher)

    @Provides
    @Singleton
    fun provideSessionIdGenerator(): SessionIdGenerator = UuidSessionIdGenerator()
}
