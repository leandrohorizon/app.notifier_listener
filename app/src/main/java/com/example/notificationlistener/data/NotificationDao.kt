package com.example.notificationlistener.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY created_at ASC LIMIT :limit")
    suspend fun getNextBatch(limit: Int): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllPendingFlow(): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM notifications 
        WHERE (:packageName IS NULL OR package_name = :packageName)
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY created_at DESC
    """)
    fun searchNotifications(query: String, packageName: String?): Flow<List<NotificationEntity>>

    @Query("SELECT DISTINCT package_name FROM notifications")
    fun getDistinctPackagesFlow(): Flow<List<String>>

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
