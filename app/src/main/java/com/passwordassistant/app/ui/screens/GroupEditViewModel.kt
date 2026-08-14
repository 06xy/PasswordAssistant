package com.passwordassistant.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.AppJson
import com.passwordassistant.app.data.FieldDefinition
import com.passwordassistant.app.data.GroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class GroupEditViewModel(
    application: Application,
    private val initialGroupId: Long,
) : AndroidViewModel(application) {
    private val database = (application as PasswordApp).container.database
    private val json = AppJson.json

    private val _group = MutableStateFlow<GroupEntity?>(null)
    val group = _group.asStateFlow()

    init {
        if (initialGroupId > 0) {
            viewModelScope.launch {
                _group.value = database.groupDao().getById(initialGroupId)
            }
        }
    }

    fun save(
        name: String,
        icon: String,
        colorIndex: Int,
        fields: List<FieldDefinition>,
        titleFieldKey: String?,
        subtitleFieldKey: String?,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val fieldsJson = json.encodeToString(fields)
            val existing = _group.value
            val id = if (existing == null) {
                database.groupDao().insert(
                    GroupEntity(
                        name = name,
                        icon = icon,
                        colorIndex = colorIndex,
                        fieldsJson = fieldsJson,
                        titleFieldKey = titleFieldKey,
                        subtitleFieldKey = subtitleFieldKey,
                    ),
                )
            } else {
                database.groupDao().update(
                    existing.copy(
                        name = name,
                        icon = icon,
                        colorIndex = colorIndex,
                        fieldsJson = fieldsJson,
                        titleFieldKey = titleFieldKey,
                        subtitleFieldKey = subtitleFieldKey,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                existing.id
            }
            onDone(id)
        }
    }

    companion object {
        fun factory(initialGroupId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PasswordApp
                GroupEditViewModel(app, initialGroupId)
            }
        }
    }
}
