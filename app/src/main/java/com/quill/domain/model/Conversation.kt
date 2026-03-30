package com.quill.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val modelConfigId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
