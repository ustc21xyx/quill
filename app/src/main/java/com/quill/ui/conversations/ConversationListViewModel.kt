package com.quill.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ConversationRepository
import com.quill.data.repository.ModelRepository
import com.quill.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = conversationRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hasModels = MutableStateFlow(false)
    val hasModels: StateFlow<Boolean> = _hasModels

    init {
        viewModelScope.launch {
            modelRepository.getAll().collect { models ->
                _hasModels.value = models.isNotEmpty()
            }
        }
    }

    fun createNewConversation(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val defaultModel = modelRepository.getDefault().first()
            val id = conversationRepository.create(modelConfigId = defaultModel?.id)
            onCreated(id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch { conversationRepository.delete(id) }
    }
}
