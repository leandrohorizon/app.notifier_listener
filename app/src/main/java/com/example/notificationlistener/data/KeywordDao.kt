package com.example.notificationlistener.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords")
    fun getAllKeywordsFlow(): Flow<List<KeywordEntity>>

    @Query("SELECT * FROM keywords")
    suspend fun getAllKeywords(): List<KeywordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyword: KeywordEntity)

    @Delete
    suspend fun delete(keyword: KeywordEntity)

    @Query("DELETE FROM keywords WHERE word = :word")
    suspend fun deleteByWord(word: String)
}
