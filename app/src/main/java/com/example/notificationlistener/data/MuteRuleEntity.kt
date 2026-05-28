package com.example.notificationlistener.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mute_rules")
data class MuteRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val package_name: String?,
    val keywords_to_mute: List<String>? = emptyList(),
    val keywords_to_bypass: List<String>? = emptyList()
)
