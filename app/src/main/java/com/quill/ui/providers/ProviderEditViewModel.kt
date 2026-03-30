package com.quill.ui.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ProviderRepository
import com.quill.domain.model.Provider
import com.quill.domain.model.RemoteModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderEditUiState(
    val displayName: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val isFetching: Boolean = false,
    val fetchedModels: List<RemoteModel>? = null,
    val fetchError: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    private val providerIdArg: String = savedStateHandle["providerId"] ?: "new"
    private val existingId: String? = if (providerIdArg == "new") null else providerIdArg

    private val _state = MutableStateFlow(ProviderEditUiState())
    val state: StateFlow<ProviderEditUiState> = _state.asStateFlow()

    init {
        if (existingId != null) {
            viewModelScope.launch {
                providerRepository.getById(existingId)?.let { provider ->
                    _state.update {
                        it.copy(
                            displayName = provider.displayName,
                            baseUrl = provider.baseUrl,
                            apiKey = provider.apiKey,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    fun updateDisplayName(value: String) = _state.update { it.copy(displayName = value) }
    fun updateBaseUrl(value: String) = _state.update { it.copy(baseUrl = value) }
    fun updateApiKey(value: String) = _state.update { it.copy(apiKey = value) }

    fun fetchModels() {
        val s = _state.value
        if (s.baseUrl.isBlank() || s.apiKey.isBlank()) {
            _state.update { it.copy(fetchError = "Please fill in URL and API Key") }
            return
        }
        _state.update { it.copy(isFetching = true, fetchError = null, fetchedModels = null) }
        viewModelScope.launch {
            val provider = Provider(
                id = existingId ?: "temp",
                displayName = s.displayName,
                baseUrl = s.baseUrl,
                apiKey = s.apiKey,
            )
            val result = providerRepository.fetchAndSyncModels(provider)
            _state.update {
                it.copy(
                    isFetching = false,
                    fetchedModels = result.getOrNull(),
                    fetchError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.displayName.isBlank() || s.baseUrl.isBlank() || s.apiKey.isBlank()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val provider = Provider(
                id = existingId ?: ProviderRepository.newId(),
                displayName = s.displayName,
                baseUrl = s.baseUrl,
                apiKey = s.apiKey,
            )
            if (existingId != null) {
                providerRepository.update(provider)
            } else {
                providerRepository.save(provider)
            }
            // Auto-fetch models after saving
            providerRepository.fetchAndSyncModels(provider)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
