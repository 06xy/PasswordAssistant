package com.passwordassistant.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.AppJson
import com.passwordassistant.app.data.EntryEntity
import com.passwordassistant.app.data.GroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class EntryEditViewModel(
    application: Application,
    private val groupId: Long,
    private val entryId: Long,
) : AndroidViewModel(application) {
    private val database = (application as PasswordApp).container.database
    private val json = AppJson.json

    private val _group = MutableStateFlow<GroupEntity?>(null)
    val group = _group.asStateFlow()

    private val _entry = MutableStateFlow<EntryEntity?>(null)
    val entry = _entry.asStateFlow()

    init {
        viewModelScope.launch {
            _group.value = database.groupDao().getById(groupId)
            if (entryId > 0) {
                _entry.value = database.entryDao().getById(entryId)
            }
        }
    }

    fun save(
        values: Map<String, String>,
        title: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val valuesJson = json.encodeToString(values)
            val existing = _entry.value
            if (existing == null) {
                database.entryDao().insert(
                    EntryEntity(
                        groupId = groupId,
                        title = title,
                        valuesJson = valuesJson,
                    ),
                )
            } else {
                database.entryDao().update(
                    existing.copy(
                        title = title,
                        valuesJson = valuesJson,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            onDone()
        }
    }

    companion object {
        fun factory(groupId: Long, entryId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PasswordApp
                EntryEditViewModel(app, groupId, entryId)
            }
        }
    }
}
