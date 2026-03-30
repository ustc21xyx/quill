package com.quill.data.repository

import com.quill.data.local.dao.ConversationDao
import com.quill.data.local.dao.MessageDao
import com.quill.data.local.entity.ConversationEntity
import com.quill.data.local.entity.MessageEntity
import com.quill.domain.model.ChatMessage
import com.quill.domain.model.Conversation
import com.quill.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) {
    fun getAll(): Flow<List<Conversation>> = conversationDao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getById(id: String): Conversation? = conversationDao.getById(id)?.toDomain()

    suspend fun create(modelConfigId: String?, title: String = "New Chat"): String {
        val id = UUID.randomUUID().toString()
        conversationDao.insert(
            ConversationEntity(id = id, title = title, modelConfigId = modelConfigId)
        )
        return id
    }

    suspend fun updateTitle(id: String, title: String) {
        conversationDao.updateTitle(id, title)
    }

    suspend fun touch(id: String) {
        conversationDao.touch(id)
    }

    suspend fun delete(id: String) {
        conversationDao.delete(id)
    }

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getByConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getMessagesSync(conversationId: String): List<ChatMessage> =
        messageDao.getByConversationSync(conversationId).map { it.toDomain() }

    suspend fun addMessage(message: ChatMessage) {
        messageDao.insert(message.toEntity())
        conversationDao.touch(message.conversationId)
        // Auto-title from first user message
        if (message.role == MessageRole.USER && messageDao.getMessageCount(message.conversationId) == 1) {
            val title = message.content.take(50).let {
                if (message.content.length > 50) "$it..." else it
            }
            conversationDao.updateTitle(message.conversationId, title)
        }
    }

    suspend fun deleteLastAssistantMessage(conversationId: String) {
        messageDao.deleteLastAssistantMessage(conversationId)
    }
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id, title = title, modelConfigId = modelConfigId,
    createdAt = createdAt, updatedAt = updatedAt,
)

private fun MessageEntity.toDomain() = ChatMessage(
    id = id, conversationId = conversationId,
    role = when (role) {
        "user" -> MessageRole.USER
        "assistant" -> MessageRole.ASSISTANT
        else -> MessageRole.SYSTEM
    },
    content = content, thinkingContent = thinkingContent,
    createdAt = createdAt, tokenCount = tokenCount,
)

private fun ChatMessage.toEntity() = MessageEntity(
    id = id, conversationId = conversationId,
    role = when (role) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
    },
    content = content, thinkingContent = thinkingContent,
    createdAt = createdAt, tokenCount = tokenCount,
)
