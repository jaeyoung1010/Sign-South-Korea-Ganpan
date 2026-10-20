package com.signkorean.app.di

import android.content.Context
import androidx.room.Room
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.port.output.ClockPort
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SignKoreanDatabase =
        Room
            .databaseBuilder(context, SignKoreanDatabase::class.java, "sign-korean.db")
            .addMigrations(SignKoreanDatabase.MIGRATION_1_2)
            .addMigrations(SignKoreanDatabase.MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideSessionTransactionStore(
        database: SignKoreanDatabase,
        clock: ClockPort,
    ): RoomSessionTransactionStore = RoomSessionTransactionStore(database, clock)
}
