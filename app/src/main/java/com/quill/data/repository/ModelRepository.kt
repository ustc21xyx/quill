package com.quill.data.repository

import com.quill.data.local.dao.ModelConfigDao
import com.quill.data.local.dao.ProviderDao
import com.quill.data.local.entity.ModelConfigEntity
import com.quill.domain.model.ModelConfig
import com.quill.domain.model.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val dao: ModelConfigDao,
    private val providerDao: ProviderDao,
) {
    fun getAll(): Flow<List<ModelConfig>> = dao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    fun getDefault(): Flow<ModelConfig?> = dao.getDefault().map { it?.toDomain() }

    suspend fun getById(id: String): ModelConfig? = dao.getById(id)?.toDomain()

    /**
     * Returns the Provider associated with a given model config.
     */
    suspend fun getProviderForModel(modelConfig: ModelConfig): Provider? {
        return providerDao.getById(modelConfig.providerId)?.let { entity ->
            Provider(
                id = entity.id,
                displayName = entity.displayName,
                baseUrl = entity.baseUrl,
                apiKey = entity.apiKey,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }

    fun getByProviderId(providerId: String): Flow<List<ModelConfig>> =
        dao.getByProviderId(providerId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun save(config: ModelConfig) {
        val entity = config.toEntity()
        dao.insert(entity)
        // If this is the first model, make it default
        if (dao.getDefaultSync() == null) {
            dao.setDefault(entity.id)
        }
    }

    suspend fun update(config: ModelConfig) {
        dao.update(config.toEntity())
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun setDefault(id: String) {
        dao.setDefault(id)
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

private fun ModelConfigEntity.toDomain() = ModelConfig(
    id = id,
    providerId = providerId,
    modelId = modelId,
    displayName = displayName,
    isDefault = isDefault,
    temperature = temperature,
    maxTokens = maxTokens,
)

private fun ModelConfig.toEntity() = ModelConfigEntity(
    id = id,
    providerId = providerId,
    modelId = modelId,
    displayName = displayName,
    isDefault = isDefault,
    temperature = temperature,
    maxTokens = maxTokens,
)
