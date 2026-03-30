package com.quill.domain.model

data class Provider(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class ModelConfig(
    val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val isDefault: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
)

data class RemoteModel(
    val id: String,
    val ownedBy: String,
)
