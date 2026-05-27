package com.example.notificationlistener.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val package_names: String?, // comma separated
    val keyword_query: String?
)
