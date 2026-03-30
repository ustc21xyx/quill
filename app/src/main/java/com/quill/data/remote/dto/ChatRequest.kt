package com.quill.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean,
    val temperature: Float? = null,
    val max_tokens: Int? = null,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)
