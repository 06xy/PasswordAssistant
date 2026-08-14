package com.passwordassistant.app.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.passwordassistant.app.data.security.VaultCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "vault")

sealed interface LockState {
    data object NoPassword : LockState
    data object Locked : LockState
    data object Unlocked : LockState
}

class VaultManager(
    private val context: Context,
    private val database: AppDatabase,
) {
    companion object {
        private const val BIOMETRIC_ALIAS = "password_assistant_biometric_key"
        private const val VERIFIER_PLAINTEXT = "password-assistant-vault-v1"
        private const val GCM_TAG_BITS = 128
        private const val IV_SIZE = 12
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val saltKey = stringPreferencesKey("salt")
    private val verifierKey = stringPreferencesKey("verifier")
    private val wrappedKeyKey = stringPreferencesKey("biometric_wrapped_key")
    private val biometricEnabledKey = stringPreferencesKey("biometric_enabled")

    private val _lockState = MutableStateFlow<LockState>(LockState.Locked)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    @Volatile
    private var vaultKey: ByteArray? = null

    private val _biometricEnrolled = MutableStateFlow(false)
    val biometricEnrolled: StateFlow<Boolean> = _biometricEnrolled.asStateFlow()

    @Volatile
    private var pendingBiometricPayload: ByteArray? = null

    init {
        scope.launch {
            val prefs = context.vaultDataStore.data.first()
            _lockState.value = if (prefs[verifierKey] != null) LockState.Locked else LockState.NoPassword
            _biometricEnrolled.value =
                prefs[biometricEnabledKey] == "1" && keystoreKeyExists()
        }
    }

    suspend fun setupMasterPassword(password: String): Boolean {
        if (password.length < 6) return false
        val salt = VaultCrypto.newSalt()
        val key = VaultCrypto.deriveKey(password, salt)
        val verifier = VaultCrypto.encrypt(key, VERIFIER_PLAINTEXT)
        context.vaultDataStore.edit { prefs ->
            prefs[saltKey] = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[verifierKey] = verifier
        }
        vaultKey = key
        _lockState.value = LockState.Unlocked
        scope.launch { migrateLegacyEntries() }
        return true
    }

    suspend fun unlock(password: String): Boolean {
        val prefs = context.vaultDataStore.data.first()
        val saltB64 = prefs[saltKey] ?: return false
        val verifier = prefs[verifierKey] ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val key = VaultCrypto.deriveKey(password, salt)
        val check = runCatching { VaultCrypto.decrypt(key, verifier) }.getOrNull()
        if (check != VERIFIER_PLAINTEXT) return false
        vaultKey = key
        _lockState.value = LockState.Unlocked
        scope.launch { migrateLegacyEntries() }
        return true
    }

    fun lock() {
        if (_lockState.value == LockState.NoPassword) return
        vaultKey = null
        pendingBiometricPayload = null
        _lockState.value = LockState.Locked
    }

    suspend fun changeMasterPassword(current: String, newPassword: String): Boolean {
        val key = vaultKey ?: return false
        if (newPassword.length < 6) return false
        val prefs = context.vaultDataStore.data.first()
        val saltB64 = prefs[saltKey] ?: return false
        val verifier = prefs[verifierKey] ?: return false
        val currentKey = VaultCrypto.deriveKey(current, Base64.decode(saltB64, Base64.NO_WRAP))
        if (runCatching { VaultCrypto.decrypt(currentKey, verifier) }.getOrNull() != VERIFIER_PLAINTEXT) {
            return false
        }

        val entries = database.entryDao().getAll()
        val newSalt = VaultCrypto.newSalt()
        val newKey = VaultCrypto.deriveKey(newPassword, newSalt)
        val newVerifier = VaultCrypto.encrypt(newKey, VERIFIER_PLAINTEXT)
        val updated = entries.map { entry ->
            val values = runCatching { VaultCrypto.decrypt(key, entry.valuesJson) }
                .getOrDefault(entry.valuesJson)
            val title = runCatching { VaultCrypto.decrypt(key, entry.title) }
                .getOrDefault(entry.title)
            entry.copy(
                title = VaultCrypto.encrypt(newKey, title),
                valuesJson = VaultCrypto.encrypt(newKey, values),
            )
        }
        database.withTransaction {
            updated.forEach { database.entryDao().update(it) }
        }
        context.vaultDataStore.edit { prefs ->
            prefs[saltKey] = Base64.encodeToString(newSalt, Base64.NO_WRAP)
            prefs[verifierKey] = newVerifier
        }
        if (keystoreKeyExists()) {
            wrapKeyWithBiometric(newKey)
        }
        vaultKey = newKey
        return true
    }

    fun biometricAvailable(): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    suspend fun enrollBiometric(): Boolean = runCatching {
        val key = vaultKey ?: error("未解锁")
        wrapKeyWithBiometric(key)
        true
    }.getOrDefault(false)

    suspend fun disableBiometric() {
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(BIOMETRIC_ALIAS)
        }
        context.vaultDataStore.edit { prefs ->
            prefs.remove(wrappedKeyKey)
            prefs.remove(biometricEnabledKey)
        }
        pendingBiometricPayload = null
        _biometricEnrolled.value = false
    }

    suspend fun createBiometricDecryptCipher(): Cipher? = runCatching {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val ksKey = ks.getKey(BIOMETRIC_ALIAS, null) as SecretKey
        val wrapped = context.vaultDataStore.data.first()[wrappedKeyKey] ?: return null
        val raw = Base64.decode(wrapped, Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, IV_SIZE)
        val ciphertext = raw.copyOfRange(IV_SIZE, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            ksKey,
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        pendingBiometricPayload = ciphertext
        cipher
    }.getOrNull()

    suspend fun finishBiometricUnlock(cipher: Cipher?): Boolean {
        if (cipher == null) return false
        return runCatching {
            val payload = pendingBiometricPayload ?: return false
            val key = cipher.doFinal(payload)
            pendingBiometricPayload = null
            val verifier = context.vaultDataStore.data.first()[verifierKey] ?: return false
            if (VaultCrypto.decrypt(key, verifier) != VERIFIER_PLAINTEXT) return false
            vaultKey = key
            _lockState.value = LockState.Unlocked
            scope.launch { migrateLegacyEntries() }
            true
        }.getOrDefault(false)
    }

    fun encryptEntryValues(plainJson: String): String {
        val key = vaultKey ?: return plainJson
        return VaultCrypto.encrypt(key, plainJson)
    }

    fun decryptEntryValues(payload: String): String {
        if (!VaultCrypto.isEncrypted(payload)) return payload
        val key = vaultKey ?: return payload
        return runCatching { VaultCrypto.decrypt(key, payload) }.getOrDefault(payload)
    }

    suspend fun migrateLegacyEntries() {
        val key = vaultKey ?: return
        val plaintextEntries = database.entryDao().getAll()
            .filter {
                !VaultCrypto.isEncrypted(it.valuesJson) ||
                    !VaultCrypto.isEncrypted(it.title)
            }
        plaintextEntries.forEach { entry ->
            val values = if (VaultCrypto.isEncrypted(entry.valuesJson)) {
                entry.valuesJson
            } else {
                VaultCrypto.encrypt(key, entry.valuesJson)
            }
            val title = if (VaultCrypto.isEncrypted(entry.title)) {
                entry.title
            } else {
                VaultCrypto.encrypt(key, entry.title)
            }
            database.entryDao().update(
                entry.copy(title = title, valuesJson = values),
            )
        }
    }

    private fun wrapKeyWithBiometric(key: ByteArray) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.deleteEntry(BIOMETRIC_ALIAS)
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        generator.init(builder.build())
        val keystoreKey = generator.generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val ciphertext = cipher.doFinal(key)
        val wrapped = Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)

        scope.launch {
            context.vaultDataStore.edit { prefs ->
                prefs[wrappedKeyKey] = wrapped
                prefs[biometricEnabledKey] = "1"
            }
            _biometricEnrolled.value = true
        }
    }

    private fun keystoreKeyExists(): Boolean = runCatching {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            .containsAlias(BIOMETRIC_ALIAS)
    }.getOrDefault(false)
}
