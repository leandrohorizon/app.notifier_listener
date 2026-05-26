package com.example.notificationlistener.data

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long,
    val package_name: String,
    val title: String,
    val content: String,
    val created_at: Long
)

fun NotificationEntity.toDto() = NotificationDto(
    id = id,
    package_name = package_name,
    title = title,
    content = content,
    created_at = created_at
)
