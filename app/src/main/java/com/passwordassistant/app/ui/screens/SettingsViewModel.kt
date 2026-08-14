package com.passwordassistant.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.SyncRepository
import com.passwordassistant.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as PasswordApp).container

    val themeMode: StateFlow<ThemeMode> = container.settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val syncServerUrl: StateFlow<String> = container.syncRepository.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncRepository.DEFAULT_SERVER_URL)
    val syncToken: StateFlow<String> = container.syncRepository.token
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val lastSyncAt: StateFlow<Long> = container.syncRepository.lastSyncAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            container.settingsRepository.setThemeMode(mode)
        }
    }

    fun exportBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            container.backupManager.exportTo(uri)
                .onSuccess { result ->
                    onResult("备份完成：${result.groups} 个分组，${result.entries} 条记录")
                }
                .onFailure { error ->
                    onResult("导出失败：${error.message}")
                }
        }
    }

    fun importBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            container.backupManager.importFrom(uri)
                .onSuccess { result ->
                    onResult("恢复完成：${result.groups} 个分组，${result.entries} 条记录")
                }
                .onFailure { error ->
                    onResult("恢复失败：${error.message}")
                }
        }
    }

    fun saveSyncConfig(serverUrl: String, token: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            container.syncRepository.saveConfig(serverUrl, token)
            onResult("云同步设置已保存")
        }
    }

    fun syncUpload(onResult: (String) -> Unit) {
        viewModelScope.launch {
            container.syncRepository.upload()
                .onSuccess { onResult("上传成功，云端快照已更新") }
                .onFailure { onResult("上传失败：${it.message}") }
        }
    }

    fun syncDownload(onResult: (String) -> Unit) {
        viewModelScope.launch {
            container.syncRepository.download()
                .onSuccess { result ->
                    onResult("下载并恢复完成：${result.groups} 个分组，${result.entries} 条记录")
                }
                .onFailure { onResult("下载失败：${it.message}") }
        }
    }
}
