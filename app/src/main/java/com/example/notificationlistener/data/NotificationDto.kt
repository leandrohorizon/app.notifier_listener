package com.example.notificationlistener.data

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long,
    val package_name: String,
    val title: String,
    val content: String,
    val category: String?,
    val channel_id: String?,
    val created_at: Long,
    val raw_metadata: String? = null
)

fun NotificationEntity.toDto() = NotificationDto(
    id = id,
    package_name = package_name,
    title = title,
    content = content,
    category = category,
    channel_id = channel_id,
    created_at = created_at,
    raw_metadata = raw_metadata
)
