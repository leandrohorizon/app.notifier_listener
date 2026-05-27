package com.example.notificationlistener.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MuteRuleDao {
    @Query("SELECT * FROM mute_rules")
    fun getAllRulesFlow(): Flow<List<MuteRuleEntity>>

    @Query("SELECT * FROM mute_rules")
    suspend fun getAllRules(): List<MuteRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MuteRuleEntity)

    @Delete
    suspend fun delete(rule: MuteRuleEntity)
}
