package com.passwordassistant.app.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.passwordassistant.app.data.LockState
import com.passwordassistant.app.ui.findActivity
import kotlinx.coroutines.launch

@Composable
fun LockScreen(vaultViewModel: VaultViewModel) {
    val lockState by vaultViewModel.lockState.collectAsState()
    if (lockState != LockState.Locked) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember { vaultViewModel.biometricAvailable() }
    val biometricEnrolled by vaultViewModel.biometricEnrolled.collectAsState()

    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun unlockWithPassword() {
        if (password.isBlank()) return
        busy = true
        error = null
        vaultViewModel.unlock(password) { ok ->
            busy = false
            if (!ok) {
                error = "主密码错误，请重试"
                password = ""
            }
        }
    }

    fun unlockWithBiometric() {
        scope.launch {
            val cipher = vaultViewModel.createBiometricDecryptCipher()
            if (cipher == null) {
                Toast.makeText(context, "生物识别不可用，请使用主密码", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val activity = context.findActivity() as? FragmentActivity
                ?: return@launch
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        val crypto = result.cryptoObject
                        scope.launch {
                            if (!vaultViewModel.finishBiometricUnlock(crypto?.cipher)) {
                                error = "解锁失败，请使用主密码"
                            }
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED
                        ) {
                            error = errString.toString()
                        }
                    }
                },
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("指纹解锁")
                    .setSubtitle("使用指纹快速解锁密码助手")
                    .setNegativeButtonText("使用主密码")
                    .build(),
                BiometricPrompt.CryptoObject(cipher),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "已锁定",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "输入主密码以解锁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("主密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                keyboardActions = KeyboardActions(
                    onDone = { unlockWithPassword() },
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = ::unlockWithPassword,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(if (busy) "验证中…" else "解锁", fontWeight = FontWeight.SemiBold)
            }
            if (biometricAvailable && biometricEnrolled) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = ::unlockWithBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("指纹解锁", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
