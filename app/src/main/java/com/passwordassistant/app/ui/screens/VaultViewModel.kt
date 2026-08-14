package com.passwordassistant.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.LockState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = (application as PasswordApp).container.vaultManager

    val lockState: StateFlow<LockState> = vault.lockState
    val biometricEnrolled: StateFlow<Boolean> = vault.biometricEnrolled

    fun setup(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(vault.setupMasterPassword(password))
        }
    }

    fun unlock(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(vault.unlock(password))
        }
    }

    fun lock() = vault.lock()

    fun biometricAvailable(): Boolean = vault.biometricAvailable()

    suspend fun createBiometricEnrollCipher(): Cipher? =
        vault.createBiometricEnrollCipher()

    suspend fun finishBiometricEnroll(cipher: Cipher?): Boolean =
        vault.finishBiometricEnroll(cipher)

    fun disableBiometric() {
        viewModelScope.launch {
            vault.disableBiometric()
        }
    }

    suspend fun createBiometricDecryptCipher(): Cipher? =
        vault.createBiometricDecryptCipher()

    suspend fun finishBiometricUnlock(cipher: Cipher?): Boolean =
        vault.finishBiometricUnlock(cipher)

    fun changePassword(current: String, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(vault.changeMasterPassword(current, newPassword))
        }
    }
}
