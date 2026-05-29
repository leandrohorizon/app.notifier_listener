package com.example.notificationlistener.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val package_name: String,
    val title: String,
    val content: String,
    val category: String? = null,
    val channel_id: String? = null,
    val is_muted: Boolean = false,
    val sub_text: String? = null,
    val created_at: Long,
    val raw_metadata: String? = null
)
