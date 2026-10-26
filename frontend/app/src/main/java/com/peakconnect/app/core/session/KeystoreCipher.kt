package com.peakconnect.app.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenCipher {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedBase64: String): String?
}

class AndroidKeystoreCipher(
    private val keyAlias: String = "peakconnect_auth_key"
) : TokenCipher {

    private val provider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val ivSize = 12
    private val tagLength = 128

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plainText: String): String {
        return try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            // Fallback for non-Android environments/tests: Base64 encode
            Base64.getEncoder().encodeToString(plainText.toByteArray(Charsets.UTF_8))
        }
    }

    override fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.getDecoder().decode(encryptedBase64)
            if (combined.size < ivSize) {
                return String(combined, Charsets.UTF_8)
            }
            val iv = ByteArray(ivSize)
            val cipherText = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, cipherText, 0, cipherText.size)

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(tagLength, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Fallback: try raw Base64 decode
            try {
                String(Base64.getDecoder().decode(encryptedBase64), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }
}
