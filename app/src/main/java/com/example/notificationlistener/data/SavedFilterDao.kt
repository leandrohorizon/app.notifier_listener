package com.example.notificationlistener.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters")
    fun getAllFiltersFlow(): Flow<List<SavedFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: SavedFilterEntity)

    @Delete
    suspend fun delete(filter: SavedFilterEntity)
}
