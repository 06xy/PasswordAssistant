package com.passwordassistant.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val groupId: Long,
    val title: String,
    val valuesJson: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun values(decrypt: (String) -> String = { it }): Map<String, String> =
        runCatching {
            AppJson.json.decodeFromString<Map<String, String>>(decrypt(valuesJson))
        }.getOrDefault(emptyMap())
}
