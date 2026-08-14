package com.passwordassistant.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.passwordassistant.app.data.FieldDefinition
import com.passwordassistant.app.data.FieldType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    navController: NavHostController,
    groupId: Long,
    entryId: Long,
    viewModel: EntryEditViewModel = viewModel(factory = EntryEditViewModel.factory(groupId, entryId)),
) {
    val group by viewModel.group.collectAsState(initial = null)
    val entry by viewModel.entry.collectAsState(initial = null)
    val fields = group?.fields().orEmpty()
    val values = remember { mutableStateMapOf<String, String>() }
    val errors = remember { mutableStateMapOf<String, Boolean>() }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(group, entry) {
        val g = group ?: return@LaunchedEffect
        if (!initialized) {
            val existing = entry?.values().orEmpty()
            g.fields().forEach { field ->
                values[field.key] = existing[field.key] ?: field.defaultValue
            }
            initialized = true
        }
    }

    fun save() {
        val g = group ?: return
        val missing = g.fields().filter { it.required && values[it.key].isNullOrBlank() }
        errors.clear()
        missing.forEach { errors[it.key] = true }
        if (missing.isNotEmpty()) return

        val fields = g.fields()
        val titleKey = g.titleFieldKey
            ?.takeIf { key -> fields.any { it.key == key } }
            ?: fields.firstOrNull()?.key
        val title = titleKey
            ?.let { values[it]?.trim() }
            ?.takeIf { it.isNotBlank() }
            ?: "未命名"
        viewModel.save(
            values = values.toMap(),
            title = title,
        ) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId > 0) "编辑记录" else "添加记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (group != null) {
                items(fields.size, key = { fields[it].key }) { index ->
                    val field = fields[index]
                    DynamicFieldInput(
                        field = field,
                        value = values[field.key] ?: "",
                        isError = errors[field.key] == true,
                        onValueChange = { newValue ->
                            values[field.key] = newValue
                            if (newValue.isNotBlank()) errors[field.key] = false
                        },
                    )
                }
            } else {
                item {
                    Text(
                        text = "加载中…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicFieldInput(
    field: FieldDefinition,
    value: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    var passwordVisible by remember(field.key) { mutableStateOf(false) }

    when (field.type) {
        FieldType.PASSWORD -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label + if (field.required) " *" else "") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("此项必填") }
                } else {
                    null
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) "隐藏" else "显示",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FieldType.NUMBER -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label + if (field.required) " *" else "") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("此项必填") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FieldType.MULTILINE -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label + if (field.required) " *" else "") },
                minLines = 3,
                isError = isError,
                supportingText = if (isError) {
                    { Text("此项必填") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FieldType.TEXT -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label + if (field.required) " *" else "") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("此项必填") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
