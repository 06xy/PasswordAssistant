package com.passwordassistant.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query(
        """
        SELECT g.*, (SELECT COUNT(*) FROM entries WHERE groupId = g.id) AS entryCount
        FROM groups g
        ORDER BY g.sortOrder, g.id
        """,
    )
    fun observeGroupsWithCount(): Flow<List<GroupWithCount>>

    @Query("SELECT * FROM groups ORDER BY sortOrder, id")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observeById(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?

    @Insert
    suspend fun insert(group: GroupEntity): Long

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun count(): Int
}
