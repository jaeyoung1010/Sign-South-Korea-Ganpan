package com.signkorean.app.di

import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.data.repository.RoomLearningRepository
import com.signkorean.domain.port.output.LearningRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LearningAdapterModule {
    @Provides
    @Singleton
    fun provideLearningRepository(
        database: SignKoreanDatabase,
        transactionStore: RoomSessionTransactionStore,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): LearningRepository = RoomLearningRepository(database, transactionStore, dispatcher)
}
