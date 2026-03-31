package com.quill.di

import android.content.Context
import androidx.room.Room
import com.quill.data.local.QuillDatabase
import com.quill.data.local.dao.ConversationDao
import com.quill.data.local.dao.MessageDao
import com.quill.data.local.dao.ModelConfigDao
import com.quill.data.local.dao.PersonaDao
import com.quill.data.local.dao.ProviderDao
import com.quill.data.remote.StreamingChatClient
import com.quill.data.remote.UpdateChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuillDatabase =
        Room.databaseBuilder(context, QuillDatabase::class.java, "quill.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideStreamingChatClient(okHttp: OkHttpClient, json: Json): StreamingChatClient =
        StreamingChatClient(okHttp, json)

    @Provides
    @Singleton
    fun provideUpdateChecker(okHttp: OkHttpClient, json: Json): UpdateChecker =
        UpdateChecker(okHttp, json)

    @Provides
    fun provideProviderDao(db: QuillDatabase): ProviderDao = db.providerDao()

    @Provides
    fun provideModelConfigDao(db: QuillDatabase): ModelConfigDao = db.modelConfigDao()

    @Provides
    fun provideConversationDao(db: QuillDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: QuillDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePersonaDao(db: QuillDatabase): PersonaDao = db.personaDao()
}
