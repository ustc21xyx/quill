package com.quill.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ModelRepository
import com.quill.data.repository.ProviderRepository
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    val providers: StateFlow<List<Provider>> = providerRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelsPerProvider: StateFlow<Map<String, List<ModelConfig>>> = modelRepository.getAll()
        .map { models -> models.groupBy { it.providerId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteProvider(id: String) {
        viewModelScope.launch { providerRepository.delete(id) }
    }

    fun refreshModels(provider: Provider) {
        viewModelScope.launch { providerRepository.fetchAndSyncModels(provider) }
    }

    fun setDefault(modelId: String) {
        viewModelScope.launch { modelRepository.setDefault(modelId) }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch { modelRepository.delete(modelId) }
    }
}
