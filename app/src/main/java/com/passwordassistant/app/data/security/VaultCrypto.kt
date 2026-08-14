package com.passwordassistant.app.data.security

import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE = 12
    private const val SALT_SIZE = 16
    private const val ARGON2_T_COST = 3
    private const val ARGON2_M_COST_KIB = 32768
    private const val ENCRYPTED_PREFIX = "enc:v1:"

    private val argon2 = Argon2Kt()

    fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }

    fun newSalt(): ByteArray = randomBytes(SALT_SIZE)

    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.encodeToByteArray(),
            salt = salt,
            tCostInIterations = ARGON2_T_COST,
            mCostInKibibyte = ARGON2_M_COST_KIB,
        )
        val buffer = result.rawHash
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    fun encrypt(key: ByteArray, plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = randomBytes(IV_SIZE)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        val ciphertext = cipher.doFinal(plaintext.encodeToByteArray())
        return ENCRYPTED_PREFIX +
            Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(key: ByteArray, payload: String): String {
        if (!isEncrypted(payload)) return payload
        val raw = Base64.decode(payload.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, IV_SIZE)
        val ciphertext = raw.copyOfRange(IV_SIZE, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return cipher.doFinal(ciphertext).decodeToString()
    }

    fun isEncrypted(payload: String): Boolean = payload.startsWith(ENCRYPTED_PREFIX)

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        MessageDigest.isEqual(a, b)
}
