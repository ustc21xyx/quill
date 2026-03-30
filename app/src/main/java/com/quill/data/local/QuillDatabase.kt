package com.quill.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quill.data.local.dao.ConversationDao
import com.quill.data.local.dao.MessageDao
import com.quill.data.local.dao.ModelConfigDao
import com.quill.data.local.dao.PersonaDao
import com.quill.data.local.dao.ProviderDao
import com.quill.data.local.entity.ConversationEntity
import com.quill.data.local.entity.MessageEntity
import com.quill.data.local.entity.ModelConfigEntity
import com.quill.data.local.entity.PersonaEntity
import com.quill.data.local.entity.ProviderEntity

@Database(
    entities = [
        ProviderEntity::class,
        ModelConfigEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PersonaEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class QuillDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun modelConfigDao(): ModelConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun personaDao(): PersonaDao
}
