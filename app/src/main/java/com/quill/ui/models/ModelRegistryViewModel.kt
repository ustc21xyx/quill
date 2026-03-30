package com.quill.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ModelRepository
import com.quill.data.repository.ProviderRepository
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelRegistryUiState(
    val models: List<ModelConfig> = emptyList(),
    val providers: Map<String, Provider> = emptyMap(),
)

@HiltViewModel
class ModelRegistryViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    val state: StateFlow<ModelRegistryUiState> = combine(
        modelRepository.getAll(),
        providerRepository.getAll(),
    ) { models, providers ->
        ModelRegistryUiState(
            models = models,
            providers = providers.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelRegistryUiState())

    fun deleteModel(id: String) {
        viewModelScope.launch { modelRepository.delete(id) }
    }

    fun setDefault(id: String) {
        viewModelScope.launch { modelRepository.setDefault(id) }
    }
}
