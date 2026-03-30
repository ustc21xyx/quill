package com.quill.data.remote

import com.quill.data.remote.dto.ChatRequest
import com.quill.domain.model.RemoteModel
import com.quill.domain.model.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class StreamingChatClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private val sseParser = SseParser(json)

    fun streamChat(
        baseUrl: String,
        apiKey: String,
        request: ChatRequest,
    ): Flow<StreamEvent> = callbackFlow {
        val url = "${baseUrl.trimEnd('/')}/v1/chat/completions"
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .build()

        val call = okHttpClient.newCall(httpRequest)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(StreamEvent.Error(e.message ?: "Network error"))
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = try {
                        response.body?.string()?.take(500) ?: "No response body"
                    } catch (_: Exception) {
                        "Could not read error body"
                    }
                    trySend(StreamEvent.Error("HTTP ${response.code}: $errorBody"))
                    close()
                    return
                }

                val inputStream = response.body?.byteStream()
                if (inputStream == null) {
                    trySend(StreamEvent.Error("Empty response body"))
                    close()
                    return
                }

                try {
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    var hasContent = false
                    val rawLines = mutableListOf<String>()

                    var line = reader.readLine()
                    while (line != null) {
                        rawLines.add(line)
                        val event = sseParser.parseLine(line)
                        if (event != null) {
                            when (event) {
                                is SseEvent.Data -> {
                                    val chunk = event.chunk
                                    for (choice in chunk.choices) {
                                        val delta = choice.delta

                                        // Handle thinking/reasoning content
                                        val thinking = delta.reasoning_content ?: delta.thinking
                                        if (thinking != null) {
                                            hasContent = true
                                            trySend(StreamEvent.ThinkingDelta(thinking))
                                        }

                                        // Handle regular content
                                        if (delta.content != null) {
                                            hasContent = true
                                            trySend(StreamEvent.ContentDelta(delta.content))
                                        }

                                        // Check for finish
                                        if (choice.finish_reason != null) {
                                            trySend(StreamEvent.Done)
                                        }
                                    }
                                }
                                is SseEvent.Done -> {
                                    trySend(StreamEvent.Done)
                                }
                                is SseEvent.ParseError -> {
                                    // Collect parse errors for debugging
                                }
                            }
                        }
                        line = reader.readLine()
                    }

                    // If no SSE content was received, try parsing as non-streaming response
                    if (!hasContent && rawLines.isNotEmpty()) {
                        val fullBody = rawLines.joinToString("\n")
                        try {
                            val jsonElement = json.parseToJsonElement(fullBody)
                            val choices = jsonElement.jsonObject["choices"]?.jsonArray
                            if (choices != null && choices.isNotEmpty()) {
                                val message = choices[0].jsonObject["message"]?.jsonObject
                                val content = message?.get("content")?.jsonPrimitive?.content
                                if (content != null) {
                                    trySend(StreamEvent.ContentDelta(content))
                                    trySend(StreamEvent.Done)
                                    hasContent = true
                                }
                            }
                            // Check for error response
                            if (!hasContent) {
                                val error = jsonElement.jsonObject["error"]?.jsonObject
                                val errorMsg = error?.get("message")?.jsonPrimitive?.content
                                if (errorMsg != null) {
                                    trySend(StreamEvent.Error("API Error: $errorMsg"))
                                } else {
                                    trySend(StreamEvent.Error("Unexpected response: ${fullBody.take(300)}"))
                                }
                            }
                        } catch (_: Exception) {
                            trySend(StreamEvent.Error("No SSE data received. Raw: ${fullBody.take(300)}"))
                        }
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        trySend(StreamEvent.Error(e.message ?: "Stream read error"))
                    }
                } finally {
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }

    suspend fun testConnection(baseUrl: String, apiKey: String): Result<String> {
        val url = "${baseUrl.trimEnd('/')}/v1/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Connected (${response.code})")
            } else {
                Result.failure(IOException("HTTP ${response.code}: ${response.body?.string()?.take(200)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchModels(baseUrl: String, apiKey: String): Result<List<RemoteModel>> {
        val url = "${baseUrl.trimEnd('/')}/v1/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(
                    IOException("HTTP ${response.code}: ${response.body?.string()?.take(200)}")
                )
            }
            val body = response.body?.string()
                ?: return Result.failure(IOException("Empty response body"))

            val jsonElement = json.parseToJsonElement(body)
            val dataArray = jsonElement.jsonObject["data"]?.jsonArray
                ?: return Result.failure(IOException("Missing 'data' field in response"))

            val models = dataArray.map { element ->
                val obj = element.jsonObject
                RemoteModel(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    ownedBy = obj["owned_by"]?.jsonPrimitive?.content ?: "",
                )
            }.filter { it.id.isNotBlank() }

            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
