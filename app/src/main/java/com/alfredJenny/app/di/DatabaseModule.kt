package com.alfredJenny.app.di

import android.content.Context
import androidx.room.Room
import com.alfredJenny.app.data.local.AppDatabase
import com.alfredJenny.app.data.local.AppDatabase.Companion.MIGRATION_1_2
import com.alfredJenny.app.data.local.AppDatabase.Companion.MIGRATION_2_3
import com.alfredJenny.app.data.local.ConversationDao
import com.alfredJenny.app.data.local.ConversationSummaryDao
import com.alfredJenny.app.data.local.MemoDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "alfred_jenny.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideConversationSummaryDao(db: AppDatabase): ConversationSummaryDao = db.conversationSummaryDao()

    @Provides
    fun provideMemoDao(db: AppDatabase): MemoDao = db.memoDao()
}
