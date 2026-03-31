package com.quill.data.repository

import com.quill.data.local.dao.PersonaDao
import com.quill.data.local.entity.PersonaEntity
import com.quill.domain.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaRepository @Inject constructor(
    private val dao: PersonaDao,
) {
    fun getAll(): Flow<List<Persona>> = dao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    fun getDefault(): Flow<Persona?> = dao.getDefault().map { it?.toDomain() }

    suspend fun getDefaultSync(): Persona? = dao.getDefaultSync()?.toDomain()

    suspend fun getById(id: String): Persona? = dao.getById(id)?.toDomain()

    suspend fun save(persona: Persona) {
        dao.insert(persona.toEntity())
        if (dao.getDefaultSync() == null) {
            dao.setDefault(persona.id)
        }
    }

    suspend fun update(persona: Persona) {
        dao.update(persona.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun setDefault(id: String) {
        dao.setDefault(id)
    }

    suspend fun clearDefault() {
        dao.clearDefault()
    }

    suspend fun ensureBuiltInPersona() {
        if (dao.getById(BUILT_IN_ID) == null) {
            dao.insert(
                PersonaEntity(
                    id = BUILT_IN_ID,
                    name = "Default Assistant",
                    description = "Built-in assistant",
                    systemPrompt = "You are a helpful assistant.",
                    isDefault = true,
                )
            )
        }
    }

    companion object {
        const val BUILT_IN_ID = "built-in-default"
        fun newId(): String = UUID.randomUUID().toString()
    }
}

private fun PersonaEntity.toDomain() = Persona(
    id = id, name = name, description = description,
    systemPrompt = systemPrompt, isDefault = isDefault,
    createdAt = createdAt, updatedAt = updatedAt,
)

private fun Persona.toEntity() = PersonaEntity(
    id = id, name = name, description = description,
    systemPrompt = systemPrompt, isDefault = isDefault,
    createdAt = createdAt, updatedAt = updatedAt,
)
