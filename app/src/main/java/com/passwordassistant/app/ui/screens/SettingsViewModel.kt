package com.passwordassistant.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as PasswordApp).container

    val themeMode: StateFlow<ThemeMode> = container.settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

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
}
