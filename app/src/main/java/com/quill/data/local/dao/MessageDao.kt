package com.quill.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quill.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getByConversationSync(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("""
        DELETE FROM messages WHERE id = (
            SELECT id FROM messages
            WHERE conversationId = :conversationId AND role = 'assistant'
            ORDER BY createdAt DESC LIMIT 1
        )
    """)
    suspend fun deleteLastAssistantMessage(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Query("""
        DELETE FROM messages WHERE conversationId = :conversationId
        AND createdAt >= (SELECT createdAt FROM messages WHERE id = :messageId)
    """)
    suspend fun deleteMessagesFrom(conversationId: String, messageId: String)
}
