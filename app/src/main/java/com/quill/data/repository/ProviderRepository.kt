package com.quill.data.repository

import com.quill.data.local.dao.ModelConfigDao
import com.quill.data.local.dao.ProviderDao
import com.quill.data.local.entity.ModelConfigEntity
import com.quill.data.local.entity.ProviderEntity
import com.quill.data.remote.StreamingChatClient
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import com.quill.domain.model.RemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val modelConfigDao: ModelConfigDao,
    private val chatClient: StreamingChatClient,
) {
    fun getAll(): Flow<List<Provider>> = providerDao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getById(id: String): Provider? = providerDao.getById(id)?.toDomain()

    suspend fun save(provider: Provider) {
        providerDao.insert(provider.toEntity())
    }

    suspend fun update(provider: Provider) {
        providerDao.update(provider.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) {
        providerDao.delete(id)
    }

    fun getModelsForProvider(providerId: String): Flow<List<ModelConfig>> {
        return modelConfigDao.getByProviderId(providerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Fetches available models from the provider's API and syncs them to the database.
     * Existing manually-added models for this provider are preserved.
     * Returns the list of remote models fetched from the API.
     */
    suspend fun fetchAndSyncModels(provider: Provider): Result<List<RemoteModel>> =
        withContext(Dispatchers.IO) {
            val result = chatClient.fetchModels(provider.baseUrl, provider.apiKey)
            result.onSuccess { remoteModels ->
                val existingModels = modelConfigDao.getByProviderIdSync(provider.id)
                val existingModelIds = existingModels.map { it.modelId }.toSet()

                val newEntities = remoteModels
                    .filter { it.id !in existingModelIds }
                    .map { remote ->
                        ModelConfigEntity(
                            id = UUID.randomUUID().toString(),
                            providerId = provider.id,
                            modelId = remote.id,
                            displayName = remote.id,
                            isDefault = false,
                        )
                    }

                if (newEntities.isNotEmpty()) {
                    modelConfigDao.insertAll(newEntities)
                }

                // If no default model exists at all, set the first one
                if (modelConfigDao.getDefaultSync() == null) {
                    val firstModel = existingModels.firstOrNull() ?: newEntities.firstOrNull()
                    firstModel?.let { modelConfigDao.setDefault(it.id) }
                }
            }
            result
        }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

private fun ProviderEntity.toDomain() = Provider(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    apiKey = apiKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Provider.toEntity() = ProviderEntity(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    apiKey = apiKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ModelConfigEntity.toDomain() = ModelConfig(
    id = id,
    providerId = providerId,
    modelId = modelId,
    displayName = displayName,
    isDefault = isDefault,
    temperature = temperature,
    maxTokens = maxTokens,
)
