package com.quill.ui.personas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.PersonaRepository
import com.quill.domain.model.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonaEditUiState(
    val name: String = "",
    val description: String = "",
    val systemPrompt: String = "",
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class PersonaEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaRepository: PersonaRepository,
) : ViewModel() {

    private val personaIdArg: String = savedStateHandle["personaId"] ?: "new"
    private val existingId: String? = if (personaIdArg == "new") null else personaIdArg

    private val _state = MutableStateFlow(PersonaEditUiState())
    val state: StateFlow<PersonaEditUiState> = _state.asStateFlow()

    init {
        if (existingId != null) {
            viewModelScope.launch {
                personaRepository.getById(existingId)?.let { persona ->
                    _state.update {
                        it.copy(
                            name = persona.name,
                            description = persona.description,
                            systemPrompt = persona.systemPrompt,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateDescription(value: String) = _state.update { it.copy(description = value) }
    fun updateSystemPrompt(value: String) = _state.update { it.copy(systemPrompt = value) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.systemPrompt.isBlank()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val persona = Persona(
                id = existingId ?: PersonaRepository.newId(),
                name = s.name,
                description = s.description,
                systemPrompt = s.systemPrompt,
            )
            if (existingId != null) {
                personaRepository.update(persona)
            } else {
                personaRepository.save(persona)
            }
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
