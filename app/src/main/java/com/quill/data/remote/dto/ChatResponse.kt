package com.quill.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatChunk(
    val id: String = "",
    val choices: List<ChunkChoice> = emptyList(),
    val usage: UsageInfo? = null,
)

@Serializable
data class ChunkChoice(
    val index: Int = 0,
    val delta: ChunkDelta = ChunkDelta(),
    val finish_reason: String? = null,
)

@Serializable
data class ChunkDelta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    // Some providers use "thinking" instead
    val thinking: String? = null,
)

@Serializable
data class UsageInfo(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0,
)
