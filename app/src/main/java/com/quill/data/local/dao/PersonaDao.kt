package com.quill.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quill.data.local.entity.PersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getById(id: String): PersonaEntity?

    @Query("SELECT * FROM personas WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): Flow<PersonaEntity?>

    @Query("SELECT * FROM personas WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSync(): PersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: PersonaEntity)

    @Update
    suspend fun update(persona: PersonaEntity)

    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE personas SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefault(id: String)

    @Query("UPDATE personas SET isDefault = 0")
    suspend fun clearDefault()
}
