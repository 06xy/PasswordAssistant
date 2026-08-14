package com.passwordassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.passwordassistant.app.data.FieldDefinition
import com.passwordassistant.app.data.FieldType
import com.passwordassistant.app.ui.Route
import com.passwordassistant.app.ui.theme.GroupVisuals
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    navController: NavHostController,
    groupId: Long,
    viewModel: GroupEditViewModel = viewModel(factory = GroupEditViewModel.factory(groupId)),
) {
    val group by viewModel.group.collectAsState(initial = null)
    val isEditing = groupId > 0

    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("folder") }
    var colorIndex by remember { mutableStateOf(6) }
    val fields = remember { mutableStateListOf<FieldDefinition>() }
    var initialized by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }
    var showFieldsError by remember { mutableStateOf(false) }
    var titleChoice by remember { mutableStateOf<String?>(null) }
    var subtitleChoice by remember { mutableStateOf<String?>(null) }

    var editingField by remember { mutableStateOf<FieldDefinition?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(group) {
        val g = group ?: return@LaunchedEffect
        if (!initialized) {
            name = g.name
            icon = g.icon
            colorIndex = g.colorIndex
            val loadedFields = g.fields()
            fields.clear()
            fields.addAll(loadedFields)
            titleChoice = g.titleFieldKey
                ?.takeIf { key -> loadedFields.any { it.key == key } }
                ?: loadedFields.firstOrNull()?.key
            subtitleChoice = g.subtitleFieldKey
                ?.let { key -> if (key.isEmpty()) "" else key.takeIf { k -> loadedFields.any { it.key == k } } }
                ?: loadedFields.getOrNull(1)?.key
            initialized = true
        }
    }

    fun save() {
        val validFields = fields.filter { it.label.isNotBlank() }
        val nameOk = name.isNotBlank()
        val fieldsOk = validFields.isNotEmpty()
        showNameError = !nameOk
        showFieldsError = !fieldsOk
        if (!nameOk || !fieldsOk) return
        viewModel.save(
            name = name.trim(),
            icon = icon,
            colorIndex = colorIndex,
            fields = validFields,
            titleFieldKey = titleChoice ?: validFields.firstOrNull()?.key,
            subtitleFieldKey = if (subtitleChoice == null) {
                validFields.getOrNull(1)?.key
            } else {
                subtitleChoice
            },
        ) { savedId ->
            if (isEditing) {
                navController.popBackStack()
            } else {
                navController.navigate(Route.groupDetail(savedId)) {
                    popUpTo(Route.Home)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑分组" else "新建分组") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = { Text("分组名称") },
                    placeholder = { Text("例如：SSH 密码") },
                    singleLine = true,
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text("请输入分组名称") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text("图标", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    GroupVisuals.icons.forEach { (key, image) ->
                        val selected = key == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        GroupVisuals.colorOf(colorIndex).copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    },
                                )
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = GroupVisuals.colorOf(colorIndex),
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = image,
                                contentDescription = key,
                                tint = if (selected) {
                                    GroupVisuals.colorOf(colorIndex)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            item {
                Text("颜色", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    GroupVisuals.colors.forEachIndexed { index, color ->
                        val selected = index == colorIndex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "列表展示",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "分组内每条记录的卡片上显示哪些信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FieldPicker(
                            label = "主标题",
                            options = fields,
                            selected = titleChoice,
                            placeholder = fields.firstOrNull()?.label ?: "无",
                            onSelect = { titleChoice = it },
                        )
                        FieldPicker(
                            label = "副标题",
                            options = fields,
                            selected = subtitleChoice,
                            placeholder = fields.getOrNull(1)?.label ?: "无",
                            allowNone = true,
                            onSelect = { subtitleChoice = it },
                        )
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "字段",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val newField = FieldDefinition(
                                key = "f_" + UUID.randomUUID().toString().take(8),
                                label = "",
                            )
                            fields.add(newField)
                            editingIndex = fields.lastIndex
                            editingField = newField
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("添加字段")
                    }
                }
            }

            if (showFieldsError) {
                item {
                    Text(
                        text = "至少需要一个字段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (fields.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = "每个字段代表一条记录里的一项。例如 SSH 分组可以包含：服务器名称、IP、端口（默认 22）、用户名、密码、备注。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = fields,
                    key = { _, field -> field.key },
                ) { index, field ->
                    FieldRow(
                        field = field,
                        canMoveUp = index > 0,
                        canMoveDown = index < fields.lastIndex,
                        onEdit = {
                            editingIndex = index
                            editingField = field
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val tmp = fields[index - 1]
                                fields[index - 1] = field
                                fields[index] = tmp
                            }
                        },
                        onMoveDown = {
                            if (index < fields.lastIndex) {
                                val tmp = fields[index + 1]
                                fields[index + 1] = field
                                fields[index] = tmp
                            }
                        },
                        onDelete = { fields.removeAt(index) },
                    )
                }
            }
        }
    }

    editingField?.let { field ->
        FieldEditorDialog(
            initial = field,
            onDismiss = {
                editingField = null
                editingIndex = -1
            },
            onSave = { updated ->
                if (editingIndex in fields.indices) {
                    fields[editingIndex] = updated
                }
                editingField = null
                editingIndex = -1
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldPicker(
    label: String,
    options: List<FieldDefinition>,
    selected: String?,
    placeholder: String,
    allowNone: Boolean = false,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = when {
        options.any { it.key == selected } -> options.first { it.key == selected }.label
        selected == "" -> "无"
        else -> placeholder
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text("无") },
                    onClick = {
                        onSelect("")
                        expanded = false
                    },
                )
            }
            options.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    onClick = {
                        onSelect(field.key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: FieldDefinition,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val fieldSuffix = buildString {
        if (field.required) append(" · 必填")
        if (field.defaultValue.isNotBlank()) append(" · 默认 ${field.defaultValue}")
    }
    Card(
        onClick = onEdit,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = field.label.ifBlank { "（未命名字段）" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = field.type.label + fieldSuffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = "上移")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = "下移")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除字段",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditorDialog(
    initial: FieldDefinition,
    onSave: (FieldDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember(initial) { mutableStateOf(initial.label) }
    var type by remember(initial) { mutableStateOf(initial.type) }
    var default by remember(initial) { mutableStateOf(initial.defaultValue) }
    var required by remember(initial) { mutableStateOf(initial.required) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("字段设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("字段名称") },
                    placeholder = { Text("例如：服务器名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("字段类型") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        FieldType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    type = option
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = default,
                    onValueChange = { default = it },
                    label = { Text("默认值（可选）") },
                    placeholder = { Text("例如：22") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "保存记录时必填",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = required,
                        onCheckedChange = { required = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank(),
                onClick = {
                    onSave(
                        FieldDefinition(
                            key = initial.key,
                            label = label.trim(),
                            type = type,
                            defaultValue = default.trim(),
                            required = required,
                        ),
                    )
                },
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
