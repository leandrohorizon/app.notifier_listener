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

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
