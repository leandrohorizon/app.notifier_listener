package com.example.notificationlistener.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_filters")
data class AppFilterEntity(
    @PrimaryKey val package_name: String,
    val is_allowed: Boolean
)
