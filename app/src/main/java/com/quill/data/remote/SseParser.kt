package com.quill.data.remote

import com.quill.data.remote.dto.ChatChunk
import kotlinx.serialization.json.Json

sealed class SseEvent {
    data class Data(val chunk: ChatChunk) : SseEvent()
    data object Done : SseEvent()
    data class ParseError(val line: String, val error: String) : SseEvent()
}

class SseParser(private val json: Json) {

    fun parseLine(line: String): SseEvent? {
        val trimmed = line.trim()

        // Skip empty lines and SSE comments
        if (trimmed.isEmpty() || trimmed.startsWith(":")) return null

        // Only process "data:" lines
        if (!trimmed.startsWith("data:")) return null

        val payload = trimmed.removePrefix("data:").trim()

        // Terminal event
        if (payload == "[DONE]") return SseEvent.Done

        return try {
            val chunk = json.decodeFromString<ChatChunk>(payload)
            SseEvent.Data(chunk)
        } catch (e: Exception) {
            SseEvent.ParseError(line, e.message ?: "Unknown parse error")
        }
    }
}
