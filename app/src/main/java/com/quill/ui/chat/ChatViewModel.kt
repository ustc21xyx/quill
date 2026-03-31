package com.quill.ui.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ChatRepository
import com.quill.data.repository.ConversationRepository
import com.quill.data.repository.ModelRepository
import com.quill.data.repository.PersonaRepository
import com.quill.domain.model.ChatMessage
import com.quill.domain.model.MessageRole
import com.quill.domain.model.StreamEvent
import com.quill.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository,
    private val modelRepository: ModelRepository,
    private val personaRepository: PersonaRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private var conversationId: String = savedStateHandle["conversationId"] ?: ""
    private val isNewChat: Boolean get() = conversationId.isEmpty()

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var currentStreamingContent = StringBuilder()
    private var currentThinkingContent = StringBuilder()

    init {
        if (!isNewChat) {
            loadConversation()
        }
        loadModels()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conversation = conversationRepository.getById(conversationId)
            if (conversation != null) {
                _state.update { it.copy(conversationTitle = conversation.title) }
            }
            // Load existing messages
            val messages = conversationRepository.getMessagesSync(conversationId)
            _state.update { state ->
                state.copy(
                    messages = messages.map { it.toUi() },
                )
            }
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            modelRepository.getAll().collect { models ->
                val defaultModel = models.firstOrNull { it.isDefault } ?: models.firstOrNull()
                _state.update {
                    it.copy(
                        availableModels = models,
                        selectedModel = it.selectedModel ?: defaultModel,
                    )
                }
            }
        }
    }

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun selectModel(modelId: String) {
        _state.update { state ->
            state.copy(selectedModel = state.availableModels.find { it.id == modelId })
        }
    }

    private suspend fun ensureConversation(): String {
        if (conversationId.isNotEmpty()) return conversationId
        val defaultModel = modelRepository.getDefault().first()
        conversationId = conversationRepository.create(modelConfigId = defaultModel?.id)
        return conversationId
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        val model = _state.value.selectedModel
        if (text.isEmpty() || model == null) return

        val editingId = _state.value.editingMessageId

        // Clear input and editing state immediately
        _state.update { it.copy(inputText = "", error = null, editingMessageId = null) }

        viewModelScope.launch {
            val convId = ensureConversation()

            // If editing, remove the edited message and everything after it
            if (editingId != null) {
                val messages = _state.value.messages
                val editIndex = messages.indexOfFirst { it.id == editingId }
                if (editIndex >= 0) {
                    _state.update { state ->
                        state.copy(messages = messages.take(editIndex).toList())
                    }
                }
                conversationRepository.deleteMessagesFrom(convId, editingId)
            }

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER,
                content = text,
            )

            // Add user message to UI
            _state.update {
                it.copy(messages = it.messages + userMessage.toUi())
            }

            // Save to DB
            conversationRepository.addMessage(userMessage)

            // Update title in UI after auto-title kicks in
            val conversation = conversationRepository.getById(convId)
            if (conversation != null) {
                _state.update { it.copy(conversationTitle = conversation.title) }
            }

            // Start streaming
            startStreaming(model)
        }
    }

    fun regenerate() {
        val model = _state.value.selectedModel ?: return
        if (_state.value.isStreaming) return

        // Remove last assistant message
        _state.update { state ->
            val messages = state.messages.toMutableList()
            val lastAssistantIndex = messages.indexOfLast { it.role == MessageRole.ASSISTANT }
            if (lastAssistantIndex >= 0) {
                messages.removeAt(lastAssistantIndex)
            }
            state.copy(messages = messages, error = null)
        }

        viewModelScope.launch {
            conversationRepository.deleteLastAssistantMessage(conversationId)
        }

        startStreaming(model)
    }

    fun editMessage(messageId: String) {
        if (_state.value.isStreaming) return

        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0) return

        val messageToEdit = messages[index]
        _state.update { state ->
            state.copy(
                inputText = messageToEdit.content,
                editingMessageId = messageId,
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun startStreaming(model: com.quill.domain.model.ModelConfig) {
        streamJob?.cancel()
        currentStreamingContent.clear()
        currentThinkingContent.clear()

        // Add placeholder assistant message
        val assistantId = UUID.randomUUID().toString()
        _state.update {
            it.copy(
                isStreaming = true,
                messages = it.messages + ChatMessageUi(
                    id = assistantId,
                    role = MessageRole.ASSISTANT,
                    content = "",
                    thinkingContent = null,
                    isStreaming = true,
                ),
            )
        }

        // Build message history for API (respect context message count setting)
        val contextCount = SettingsViewModel.getContextMessageCount(appContext)
        val historyMessages = _state.value.messages
            .filter { !it.isStreaming }
            .takeLast(contextCount)
            .map {
                ChatMessage(
                    id = it.id,
                    conversationId = conversationId,
                    role = it.role,
                    content = it.content,
                )
            }

        streamJob = viewModelScope.launch {
            // Prepend system prompt from default persona
            val persona = personaRepository.getDefaultSync()
            val messagesWithSystem = if (persona != null) {
                listOf(
                    ChatMessage(
                        id = "system",
                        conversationId = conversationId,
                        role = MessageRole.SYSTEM,
                        content = persona.systemPrompt,
                    )
                ) + historyMessages
            } else {
                historyMessages
            }
            val provider = modelRepository.getProviderForModel(model)
            if (provider == null) {
                _state.update {
                    it.copy(
                        isStreaming = false,
                        error = "Provider not found for model ${model.displayName}",
                        messages = it.messages.filter { msg -> msg.id != assistantId },
                    )
                }
                return@launch
            }
            var finalized = false
            chatRepository.streamCompletion(messagesWithSystem, model, provider).collect { event ->
                when (event) {
                    is StreamEvent.ContentDelta -> {
                        currentStreamingContent.append(event.text)
                        updateStreamingMessage(assistantId)
                    }
                    is StreamEvent.ThinkingDelta -> {
                        currentThinkingContent.append(event.text)
                        updateStreamingMessage(assistantId)
                    }
                    is StreamEvent.Done -> {
                        if (!finalized) {
                            finalized = true
                            finalizeMessage(assistantId)
                        }
                    }
                    is StreamEvent.Error -> {
                        _state.update {
                            it.copy(
                                isStreaming = false,
                                error = event.message,
                                messages = it.messages.filter { msg -> msg.id != assistantId },
                            )
                        }
                    }
                }
            }
            // Safety net: if stream ended without Done event, finalize anyway
            if (!finalized && _state.value.isStreaming) {
                finalizeMessage(assistantId)
            }
        }
    }

    private fun updateStreamingMessage(messageId: String) {
        _state.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            content = currentStreamingContent.toString(),
                            thinkingContent = currentThinkingContent.toString().ifEmpty { null },
                        )
                    } else msg
                },
            )
        }
    }

    private fun finalizeMessage(messageId: String) {
        val content = currentStreamingContent.toString()
        val thinking = currentThinkingContent.toString().ifEmpty { null }

        _state.update { state ->
            state.copy(
                isStreaming = false,
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            content = content,
                            thinkingContent = thinking,
                            isStreaming = false,
                        )
                    } else msg
                },
            )
        }

        // Save to DB
        viewModelScope.launch {
            conversationRepository.addMessage(
                ChatMessage(
                    id = messageId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    thinkingContent = thinking,
                )
            )
        }

        currentStreamingContent.clear()
        currentThinkingContent.clear()
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        // Finalize whatever content we have so far
        val streamingMsg = _state.value.messages.lastOrNull { it.isStreaming }
        if (streamingMsg != null) {
            finalizeMessage(streamingMsg.id)
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}

private fun ChatMessage.toUi() = ChatMessageUi(
    id = id,
    role = role,
    content = content,
    thinkingContent = thinkingContent,
    timestamp = createdAt,
)
