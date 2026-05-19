package com.realfilters.app.data.db

import androidx.room.*
import com.realfilters.app.data.model.SavedFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    fun getAllFilters(): Flow<List<SavedFilter>>

    @Query("SELECT * FROM saved_filters WHERE id = :id")
    suspend fun getFilterById(id: Long): SavedFilter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: SavedFilter): Long

    @Update
    suspend fun updateFilter(filter: SavedFilter)

    @Delete
    suspend fun deleteFilter(filter: SavedFilter)

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun deleteFilterById(id: Long)
}
