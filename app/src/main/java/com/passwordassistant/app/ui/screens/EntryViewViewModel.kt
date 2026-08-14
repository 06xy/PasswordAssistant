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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EntryViewViewModel(
    application: Application,
    groupId: Long,
    entryId: Long,
) : AndroidViewModel(application) {
    private val database = (application as PasswordApp).container.database
    private val vaultManager = (application as PasswordApp).container.vaultManager

    private val _group = MutableStateFlow<GroupEntity?>(null)
    val group = _group.asStateFlow()

    private val _entry = MutableStateFlow<EntryEntity?>(null)
    val entry = _entry.asStateFlow()

    init {
        viewModelScope.launch {
            _group.value = database.groupDao().getById(groupId)
            _entry.value = database.entryDao().getById(entryId)
        }
    }

    fun decryptEntryValues(payload: String): String =
        vaultManager.decryptEntryValues(payload)

    companion object {
        fun factory(groupId: Long, entryId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PasswordApp
                EntryViewViewModel(app, groupId, entryId)
            }
        }
    }
}
