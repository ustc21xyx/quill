package com.quill.ui.chat

import com.quill.domain.model.MessageRole
import com.quill.domain.model.ModelConfig

data class ChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val isStreaming: Boolean = false,
    val inputText: String = "",
    val selectedModel: ModelConfig? = null,
    val availableModels: List<ModelConfig> = emptyList(),
    val error: String? = null,
    val conversationTitle: String = "New Chat",
    val editingMessageId: String? = null,
)

data class ChatMessageUi(
    val id: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = 0L,
)
