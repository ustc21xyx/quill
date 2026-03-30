package com.quill.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ProviderRepository
import com.quill.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    val providers: StateFlow<List<Provider>> = providerRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteProvider(id: String) {
        viewModelScope.launch { providerRepository.delete(id) }
    }

    fun refreshModels(provider: Provider) {
        viewModelScope.launch { providerRepository.fetchAndSyncModels(provider) }
    }
}
