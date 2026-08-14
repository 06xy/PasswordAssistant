package com.passwordassistant.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE groupId = :groupId ORDER BY sortOrder, id")
    fun observeByGroup(groupId: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY sortOrder, id")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)
}
