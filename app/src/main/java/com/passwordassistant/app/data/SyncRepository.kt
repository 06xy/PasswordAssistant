package com.passwordassistant.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.passwordassistant.app.data.security.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync")

class SyncRepository(
    private val context: Context,
    private val backupManager: BackupManager,
    private val vaultManager: VaultManager,
) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://backup.06xy.cn"
    }

    private val serverUrlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("token")
    private val lastSyncKey = longPreferencesKey("last_sync_at")

    val serverUrl: Flow<String> = context.syncDataStore.data.map {
        it[serverUrlKey] ?: DEFAULT_SERVER_URL
    }
    val token: Flow<String> = context.syncDataStore.data.map { it[tokenKey] ?: "" }
    val lastSyncAt: Flow<Long> = context.syncDataStore.data.map { it[lastSyncKey] ?: 0L }

    suspend fun saveConfig(serverUrl: String, token: String) {
        context.syncDataStore.edit { prefs ->
            prefs[serverUrlKey] = serverUrl.trim().trimEnd('/')
            prefs[tokenKey] = token.trim()
        }
    }

    suspend fun upload(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = context.syncDataStore.data.first()[serverUrlKey]
                ?.trim()
                ?.trimEnd('/')
                ?: DEFAULT_SERVER_URL
            val token = context.syncDataStore.data.first()[tokenKey] ?: ""
            if (token.isBlank()) error("请先在设置中填写同步令牌")
            val vaultKey = vaultManager.currentKey() ?: error("请先解锁")
            val bytes = backupManager.buildBackupBytes()
            val encrypted = VaultCrypto.encrypt(
                vaultKey,
                Base64.encodeToString(bytes, Base64.NO_WRAP),
            )
            val body = JSONObject()
                .put("data", encrypted)
                .put("updatedAt", System.currentTimeMillis())
                .toString()

            val conn = URL("$url/api/vault").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.encodeToByteArray()) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    error("上传失败（HTTP $code）")
                }
            } finally {
                conn.disconnect()
            }
            context.syncDataStore.edit {
                it[lastSyncKey] = System.currentTimeMillis()
            }
            Unit
        }
    }

    suspend fun download(): Result<BackupResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = context.syncDataStore.data.first()[serverUrlKey]
                ?.trim()
                ?.trimEnd('/')
                ?: DEFAULT_SERVER_URL
            val token = context.syncDataStore.data.first()[tokenKey] ?: ""
            if (token.isBlank()) error("请先在设置中填写同步令牌")

            val conn = URL("$url/api/vault").openConnection() as HttpURLConnection
            val (encrypted, updatedAt) = try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("Authorization", "Bearer $token")
                val code = conn.responseCode
                if (code == 404) error("云端暂无备份")
                if (code !in 200..299) error("下载失败（HTTP $code）")
                val body = conn.inputStream.use { it.readBytes().decodeToString() }
                val obj = JSONObject(body)
                obj.getString("data") to obj.optLong("updatedAt")
            } finally {
                conn.disconnect()
            }

            val vaultKey = vaultManager.currentKey() ?: error("请先解锁")
            val plain = VaultCrypto.decrypt(vaultKey, encrypted)
            val bytes = Base64.decode(plain, Base64.NO_WRAP)
            val result = backupManager.importBytes(bytes)
            context.syncDataStore.edit {
                it[lastSyncKey] = if (updatedAt > 0) updatedAt else System.currentTimeMillis()
            }
            result
        }
    }
}
