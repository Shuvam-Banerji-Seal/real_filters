package com.realfilters.app.di

import android.content.Context
import androidx.room.Room
import com.realfilters.app.data.db.AppDatabase
import com.realfilters.app.data.db.FilterDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "realfilters.db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideFilterDao(database: AppDatabase): FilterDao {
        return database.filterDao()
    }
}
