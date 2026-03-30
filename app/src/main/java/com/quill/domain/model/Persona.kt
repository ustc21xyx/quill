package com.quill.domain.model

data class Persona(
    val id: String,
    val name: String,
    val description: String = "",
    val systemPrompt: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
