package com.passwordassistant.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.passwordassistant.app.data.SyncRepository
import com.passwordassistant.app.data.ThemeMode
import com.passwordassistant.app.ui.launchBiometricAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val biometricEnrolled by vaultViewModel.biometricEnrolled.collectAsState()
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showSyncRestoreConfirm by remember { mutableStateOf(false) }
    val biometricAvailable = remember { vaultViewModel.biometricAvailable() }
    val scope = rememberCoroutineScope()
    val syncServerUrl by viewModel.syncServerUrl.collectAsState()
    val syncTokenStored by viewModel.syncToken.collectAsState()
    val lastSyncAt by viewModel.lastSyncAt.collectAsState()
    var syncServerInput by remember { mutableStateOf("") }
    var syncTokenInput by remember { mutableStateOf("") }
    LaunchedEffect(syncServerUrl) { syncServerInput = syncServerUrl }
    LaunchedEffect(syncTokenStored) { syncTokenInput = syncTokenStored }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(it) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreConfirm = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                SectionTitle("安全")
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("修改主密码") },
                            supportingContent = { Text("修改后全部记录将用新密码重新加密") },
                            leadingContent = {
                                Icon(Icons.Outlined.VpnKey, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                showChangePassword = true
                            },
                        )
                        if (biometricAvailable) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            )
                            ListItem(
                                headlineContent = { Text("指纹解锁") },
                                supportingContent = { Text("用指纹快速解锁（密钥保存在系统安全芯片）") },
                                leadingContent = {
                                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                                },
                                trailingContent = {
                                    Switch(
                                        checked = biometricEnrolled,
                                        onCheckedChange = { enable ->
                                            if (enable) {
                                                scope.launch {
                                                    val cipher = vaultViewModel.createBiometricEnrollCipher()
                                                    if (cipher == null) {
                                                        Toast.makeText(
                                                            context,
                                                            "启用失败：请确认已录入指纹且未锁定应用",
                                                            Toast.LENGTH_LONG,
                                                        ).show()
                                                        return@launch
                                                    }
                                                    launchBiometricAuth(
                                                        context = context,
                                                        title = "启用指纹解锁",
                                                        subtitle = "验证指纹后，保险库密钥将交给系统安全芯片保管",
                                                        negativeButtonText = "取消",
                                                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                        onSuccess = { result ->
                                                            scope.launch {
                                                                val ok = vaultViewModel.finishBiometricEnroll(
                                                                    result.cryptoObject?.cipher,
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    if (ok) {
                                                                        "指纹解锁已启用"
                                                                    } else {
                                                                        "启用失败，请重试"
                                                                    },
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            }
                                                        },
                                                        onError = { message ->
                                                            Toast.makeText(
                                                                context,
                                                                "启用失败：$message",
                                                                Toast.LENGTH_SHORT,
                                                            ).show()
                                                        },
                                                    )
                                                }
                                            } else {
                                                vaultViewModel.disableBiometric()
                                                Toast.makeText(
                                                    context,
                                                    "指纹解锁已关闭",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        },
                                    )
                                },
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                        ListItem(
                            headlineContent = { Text("立即锁定") },
                            supportingContent = { Text("清空内存中的密钥并回到锁屏") },
                            leadingContent = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                vaultViewModel.lock()
                            },
                        )
                    }
                }
            }

            item {
                SectionTitle("云同步")
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "本地快照使用主密码派生的密钥加密后上传，服务器只能存取密文，无法解密。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = syncServerInput,
                            onValueChange = { syncServerInput = it },
                            label = { Text("服务器地址") },
                            placeholder = { Text(SyncRepository.DEFAULT_SERVER_URL) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = syncTokenInput,
                            onValueChange = { syncTokenInput = it },
                            label = { Text("同步令牌") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.saveSyncConfig(syncServerInput, syncTokenInput) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ) {
                                Text("保存设置")
                            }
                            TextButton(
                                onClick = {
                                    viewModel.syncUpload { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                            ) {
                                Text("上传快照")
                            }
                            TextButton(
                                onClick = { showSyncRestoreConfirm = true },
                            ) {
                                Text("下载恢复")
                            }
                        }
                        if (lastSyncAt > 0) {
                            Text(
                                text = "上次同步：" + SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm",
                                    Locale.getDefault(),
                                ).format(Date(lastSyncAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("外观")
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                )
                                Text(
                                    text = mode.label,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("数据")
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("导出备份") },
                            supportingContent = { Text("将全部分组、记录与设置导出为 zip 文件") },
                            leadingContent = {
                                Icon(Icons.Outlined.Upload, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                exportLauncher.launch("password_assistant_backup.zip")
                            },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                        ListItem(
                            headlineContent = { Text("从备份恢复") },
                            supportingContent = { Text("导入 zip 并替换当前全部数据") },
                            leadingContent = {
                                Icon(Icons.Outlined.Download, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                importLauncher.launch(
                                    arrayOf("application/zip", "application/octet-stream"),
                                )
                            },
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("关于")
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    ListItem(
                        headlineContent = { Text("密码助手") },
                        supportingContent = { Text("版本 0.4.1 · 支持云同步") },
                        leadingContent = {
                            Icon(Icons.Outlined.Lock, contentDescription = null)
                        },
                    )
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            },
            title = { Text("恢复备份") },
            text = { Text("恢复将替换当前所有分组和记录，此操作不可撤销。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri?.let { uri ->
                            viewModel.importBackup(uri) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                        pendingRestoreUri = null
                    },
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onChange = { current, newPassword ->
                vaultViewModel.changePassword(current, newPassword) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) "主密码已修改" else "修改失败：请检查当前密码或新密码长度",
                        Toast.LENGTH_LONG,
                    ).show()
                    if (ok) {
                        showChangePassword = false
                    }
                }
            },
        )
    }

    if (showSyncRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showSyncRestoreConfirm = false },
            title = { Text("下载并恢复") },
            text = { Text("将用云端快照替换当前全部分组和记录，此操作不可撤销。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSyncRestoreConfirm = false
                        viewModel.syncDownload { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                ) {
                    Text("下载恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncRestoreConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChange: (current: String, newPassword: String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改主密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码（至少 6 位）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("确认新密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (newPassword.length < 6) {
                        error = "新密码至少需要 6 位"
                        return@TextButton
                    }
                    if (newPassword != confirm) {
                        error = "两次输入的新密码不一致"
                        return@TextButton
                    }
                    busy = true
                    error = null
                    onChange(current, newPassword)
                },
            ) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}
