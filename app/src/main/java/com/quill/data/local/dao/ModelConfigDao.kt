package com.quill.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.quill.data.local.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {

    @Query("SELECT * FROM model_configs ORDER BY displayName ASC")
    fun getAll(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getById(id: String): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId ORDER BY displayName ASC")
    fun getByProviderId(providerId: String): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId ORDER BY displayName ASC")
    suspend fun getByProviderIdSync(providerId: String): List<ModelConfigEntity>

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): Flow<ModelConfigEntity?>

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSync(): ModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ModelConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ModelConfigEntity>)

    @Update
    suspend fun update(config: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM model_configs WHERE providerId = :providerId")
    suspend fun deleteByProviderId(providerId: String)

    @Transaction
    suspend fun setDefault(id: String) {
        clearDefault()
        markDefault(id)
    }

    @Query("UPDATE model_configs SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefault()

    @Query("UPDATE model_configs SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: String)
}
