package com.example.notificationlistener.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFilterDao {
    @Query("SELECT * FROM app_filters")
    fun getAllFiltersFlow(): Flow<List<AppFilterEntity>>

    @Query("SELECT * FROM app_filters")
    suspend fun getAllFilters(): List<AppFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: AppFilterEntity)

    @Delete
    suspend fun delete(filter: AppFilterEntity)

    @Query("DELETE FROM app_filters WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
