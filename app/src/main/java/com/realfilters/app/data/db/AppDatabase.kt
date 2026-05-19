package com.realfilters.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.realfilters.app.data.model.SavedFilter

@Database(entities = [SavedFilter::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
}
