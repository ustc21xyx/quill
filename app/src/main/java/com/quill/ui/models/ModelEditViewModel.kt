package com.quill.ui.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ChatRepository
import com.quill.data.repository.ModelRepository
import com.quill.data.repository.ProviderRepository
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelEditUiState(
    val displayName: String = "",
    val modelId: String = "",
    val providerId: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val providers: List<Provider> = emptyList(),
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ModelEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modelRepository: ModelRepository,
    private val providerRepository: ProviderRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val modelIdArg: String = savedStateHandle["modelId"] ?: "new"
    private val existingId: String? = if (modelIdArg == "new") null else modelIdArg
    private val preselectedProviderId: String? = savedStateHandle["providerId"]

    private val _state = MutableStateFlow(ModelEditUiState())
    val state: StateFlow<ModelEditUiState> = _state.asStateFlow()

    init {
        // If a providerId was passed via navigation, pre-select it
        if (preselectedProviderId != null) {
            _state.update { it.copy(providerId = preselectedProviderId) }
        }
        loadProviders()
        if (existingId != null) {
            viewModelScope.launch {
                modelRepository.getById(existingId)?.let { config ->
                    _state.update {
                        it.copy(
                            displayName = config.displayName,
                            modelId = config.modelId,
                            providerId = config.providerId,
                            temperature = config.temperature,
                            maxTokens = config.maxTokens,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            providerRepository.getAll().collect { providers ->
                _state.update { state ->
                    val resolvedProviderId = when {
                        state.providerId.isNotEmpty() -> state.providerId
                        preselectedProviderId != null -> preselectedProviderId
                        else -> providers.firstOrNull()?.id ?: ""
                    }
                    state.copy(
                        providers = providers,
                        providerId = resolvedProviderId,
                    )
                }
            }
        }
    }

    fun updateDisplayName(value: String) = _state.update { it.copy(displayName = value) }
    fun updateModelId(value: String) = _state.update { it.copy(modelId = value) }
    fun updateProviderId(value: String) = _state.update { it.copy(providerId = value) }
    fun updateTemperature(value: Float) = _state.update { it.copy(temperature = value) }
    fun updateMaxTokens(value: Int) = _state.update { it.copy(maxTokens = value.coerceIn(1, 128000)) }

    fun testConnection() {
        val s = _state.value
        val provider = s.providers.find { it.id == s.providerId }
        if (provider == null) {
            _state.update { it.copy(testResult = "Please select a provider") }
            return
        }
        _state.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            val result = chatRepository.testConnection(provider)
            _state.update {
                it.copy(
                    isTesting = false,
                    testResult = result.fold(
                        onSuccess = { msg -> msg },
                        onFailure = { err -> "Failed: ${err.message}" },
                    ),
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.displayName.isBlank() || s.modelId.isBlank() || s.providerId.isBlank()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val config = ModelConfig(
                id = existingId ?: ModelRepository.newId(),
                providerId = s.providerId,
                modelId = s.modelId,
                displayName = s.displayName,
                temperature = s.temperature,
                maxTokens = s.maxTokens,
            )
            if (existingId != null) {
                modelRepository.update(config)
            } else {
                modelRepository.save(config)
            }
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
