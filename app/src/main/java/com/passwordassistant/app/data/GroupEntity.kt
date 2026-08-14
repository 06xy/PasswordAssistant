package com.passwordassistant.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val icon: String,
    val colorIndex: Int,
    val fieldsJson: String,
    val titleFieldKey: String? = null,
    val subtitleFieldKey: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun fields(): List<FieldDefinition> =
        runCatching {
            AppJson.json.decodeFromString<List<FieldDefinition>>(fieldsJson)
        }.getOrDefault(emptyList())
}
