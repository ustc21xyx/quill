package com.quill.domain.model

enum class MessageRole { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val tokenCount: Int? = null,
)
