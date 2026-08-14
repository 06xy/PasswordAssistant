package com.passwordassistant.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.EntryEntity
import com.passwordassistant.app.data.GroupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    application: Application,
    private val groupId: Long,
) : AndroidViewModel(application) {
    private val database = (application as PasswordApp).container.database
    private val vaultManager = (application as PasswordApp).container.vaultManager

    val group: Flow<GroupEntity?> = database.groupDao().observeById(groupId)
    val entries: Flow<List<EntryEntity>> = database.entryDao().observeByGroup(groupId)

    fun deleteGroup() {
        viewModelScope.launch {
            database.groupDao().getById(groupId)?.let { group ->
                database.groupDao().delete(group)
            }
        }
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch {
            database.entryDao().delete(entry)
        }
    }

    fun decryptEntryValues(payload: String): String =
        vaultManager.decryptEntryValues(payload)

    companion object {
        fun factory(groupId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PasswordApp
                GroupDetailViewModel(app, groupId)
            }
        }
    }
}
