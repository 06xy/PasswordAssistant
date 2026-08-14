package com.passwordassistant.app.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val app: String = "PasswordAssistant",
    val formatVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class BackupSettings(
    val themeMode: String,
)

@Serializable
data class BackupGroups(
    val groups: List<GroupEntity>,
)

@Serializable
data class BackupEntries(
    val entries: List<EntryEntity>,
)
