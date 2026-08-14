package com.passwordassistant.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.passwordassistant.app.data.FieldDefinition
import com.passwordassistant.app.data.FieldType
import com.passwordassistant.app.ui.Route
import com.passwordassistant.app.ui.theme.GroupVisuals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryViewScreen(
    navController: NavHostController,
    groupId: Long,
    entryId: Long,
    viewModel: EntryViewViewModel = viewModel(factory = EntryViewViewModel.factory(groupId, entryId)),
) {
    val group by viewModel.group.collectAsState(initial = null)
    val entry by viewModel.entry.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fields = group?.fields().orEmpty()
    val values = entry?.values(viewModel::decryptEntryValues).orEmpty()
    val title = entry?.let { viewModel.decryptEntryValues(it.title) } ?: "记录详情"
    var passwordVisible by remember { mutableStateOf(false) }

    fun copyValue(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制，30 秒后自动清除", Toast.LENGTH_SHORT).show()
        scope.launch {
            delay(30_000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Route.entryEdit(groupId, entryId)) },
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑记录")
                    }
                },
            )
        },
    ) { padding ->
        if (group == null || entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "加载中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = GroupVisuals.colorOf(group?.colorIndex ?: 0),
                            )
                        }
                        Text(
                            text = group?.name ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(fields.size, key = { fields[it].key }) { index ->
                    val field = fields[index]
                    val value = values[field.key].orEmpty()
                    FieldValueRow(
                        field = field,
                        value = value,
                        passwordVisible = passwordVisible && field.type == FieldType.PASSWORD,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onCopy = { copyValue(field.label, value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldValueRow(
    field: FieldDefinition,
    value: String,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val display = when {
                    value.isBlank() -> "（空）"
                    field.type == FieldType.PASSWORD && !passwordVisible -> "••••••••"
                    else -> value
                }
                Text(
                    text = display,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (field.type == FieldType.PASSWORD && value.isNotBlank()) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) "隐藏" else "显示",
                        )
                    }
                }
                if (value.isNotBlank()) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制${field.label}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
