package com.example.notificationlistener.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mute_rules")
data class MuteRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val package_name: String,
    val category: String?,
    val channel_id: String?,
    val text_keyword: String? = null
)
