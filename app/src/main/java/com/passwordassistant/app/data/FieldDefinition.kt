package com.passwordassistant.app.data

import kotlinx.serialization.Serializable

@Serializable
data class FieldDefinition(
    val key: String,
    val label: String,
    val type: FieldType = FieldType.TEXT,
    val defaultValue: String = "",
    val required: Boolean = false,
)
