package com.passwordassistant.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResult(
    val groups: Int,
    val entries: Int,
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val vaultManager: VaultManager,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportTo(uri: Uri): Result<BackupResult> = runCatching {
        val groups = database.groupDao().getAll()
        val entries = database.entryDao().getAll()
        val settings = settingsRepository.themeMode.first()

        val files = linkedMapOf(
            "manifest.json" to json.encodeToString(
                BackupManifest.serializer(),
                BackupManifest(createdAt = System.currentTimeMillis()),
            ),
            "settings.json" to json.encodeToString(
                BackupSettings.serializer(),
                BackupSettings(themeMode = settings.name),
            ),
            "groups.json" to json.encodeToString(BackupGroups.serializer(), BackupGroups(groups)),
            "entries.json" to json.encodeToString(BackupEntries.serializer(), BackupEntries(entries)),
        )

        val output = context.contentResolver.openOutputStream(uri)
            ?: error("无法写入所选位置")
        output.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                files.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.encodeToByteArray())
                    zip.closeEntry()
                }
            }
        }
        BackupResult(groups.size, entries.size)
    }

    suspend fun importFrom(uri: Uri): Result<BackupResult> = runCatching {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")
        val files = mutableMapOf<String, String>()
        stream.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        files[entry.name] = zip.readBytes().decodeToString()
                    }
                    entry = zip.nextEntry
                }
            }
        }

        val manifest = files["manifest.json"]?.let {
            json.decodeFromString<BackupManifest>(it)
        } ?: error("备份文件缺少 manifest.json")
        if (manifest.formatVersion != 1) {
            error("不支持的备份版本：${manifest.formatVersion}")
        }
        val backupGroups = files["groups.json"]?.let {
            json.decodeFromString<BackupGroups>(it)
        } ?: error("备份文件缺少 groups.json")
        val backupEntries = files["entries.json"]?.let {
            json.decodeFromString<BackupEntries>(it)
        } ?: error("备份文件缺少 entries.json")
        val backupSettings = files["settings.json"]?.let {
            json.decodeFromString<BackupSettings>(it)
        }

        database.withTransaction {
            val groupDao = database.groupDao()
            val entryDao = database.entryDao()

            groupDao.getAll().forEach { groupDao.delete(it) }

            val idMap = mutableMapOf<Long, Long>()
            backupGroups.groups.forEach { group ->
                val newId = groupDao.insert(group.copy(id = 0L))
                idMap[group.id] = newId
            }
            backupEntries.entries.forEach { entry ->
                val mappedGroupId = idMap[entry.groupId] ?: return@forEach
                entryDao.insert(entry.copy(id = 0L, groupId = mappedGroupId))
            }
        }

        backupSettings?.let { settings ->
            val mode = runCatching { ThemeMode.valueOf(settings.themeMode) }
                .getOrDefault(ThemeMode.SYSTEM)
            settingsRepository.setThemeMode(mode)
        }
        vaultManager.migrateLegacyEntries()

        BackupResult(backupGroups.groups.size, backupEntries.entries.size)
    }
}
