package com.quill.data.repository

import com.quill.data.remote.StreamingChatClient
import com.quill.data.remote.dto.ChatMessageDto
import com.quill.data.remote.dto.ChatRequest
import com.quill.domain.model.ChatMessage
import com.quill.domain.model.MessageRole
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import com.quill.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val client: StreamingChatClient,
) {
    fun streamCompletion(
        messages: List<ChatMessage>,
        modelConfig: ModelConfig,
        provider: Provider,
    ): Flow<StreamEvent> {
        val request = ChatRequest(
            model = modelConfig.modelId,
            messages = messages.map { msg ->
                ChatMessageDto(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content,
                )
            },
            temperature = modelConfig.temperature,
            max_tokens = modelConfig.maxTokens,
        )
        return client.streamChat(
            baseUrl = provider.baseUrl,
            apiKey = provider.apiKey,
            request = request,
        )
    }

    suspend fun testConnection(provider: Provider): Result<String> {
        return client.testConnection(provider.baseUrl, provider.apiKey)
    }
}
