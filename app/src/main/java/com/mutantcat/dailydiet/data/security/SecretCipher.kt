package com.mutantcat.dailydiet.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 用 Android Keystore 里的 AES/GCM 密钥加解密敏感字符串（目前只有 AI API Key）。
 *
 * 密钥由系统生成并保存在 Keystore 中，进程拿不到明文密钥；备份到别的设备也无法解开，
 * 这与应用「无云同步、allowBackup=false」的定位一致。
 *
 * 密文格式：`base64(iv) + ":" + base64(ciphertext)`，每次加密使用新的随机 IV。
 */
class SecretCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    fun encrypt(plainText: String): String? = runCatching {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val body = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        "$iv:$body"
    }.getOrNull()

    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        // 兼容明文历史数据：没有分隔符就原样返回，下次保存时会自动加密。
        val separator = stored.indexOf(':')
        if (separator <= 0) return stored
        return runCatching {
            val iv = Base64.decode(stored.substring(0, separator), Base64.NO_WRAP)
            val body = Base64.decode(stored.substring(separator + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "daily_diet_ai_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
