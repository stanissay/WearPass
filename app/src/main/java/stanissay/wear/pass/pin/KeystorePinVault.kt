/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass.pin

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.PinResult
import stanissay.wear.pass.utils.PinVault
import stanissay.wear.pass.utils.keyStore
import stanissay.wear.pass.utils.toChars
import stanissay.wear.pass.utils.toUtf8
import stanissay.wear.pass.utils.writeAtomic
import java.io.File
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeystorePinVault(private val blobFile: File, private val metaFile: File, private val alias: String) :
    PinVault {
    override suspend fun setup(pin: CharArray, password: CharArray) = withContext(Dispatchers.Default) { doSetup(pin, password) }
    override suspend fun unlock(pin: CharArray): PinResult = withContext(Dispatchers.Default) { doUnlock(pin) }
    override fun wipe() {
        blobFile.delete()
        metaFile.delete()
        runCatching { keyStore().deleteEntry(alias) }
    }

    private fun doSetup(pin: CharArray, password: CharArray) {
        val salt = ByteArray(DataConstants.SALT_LEN).also { SecureRandom().nextBytes(it) }
        val plain = password.toUtf8()
        try {
            val inner = encrypt(derive(pin, salt), plain)
            val outer = encrypt(createKeystoreKey(), inner)
            writeAtomic(blobFile, byteArrayOf(DataConstants.VERSION) + salt + outer)
            writeAttempts(0)
        } finally {
            plain.fill(0)
        }
    }

    private fun doUnlock(pin: CharArray): PinResult {
        if (!blobFile.exists()) return PinResult.TooMany

        val used = (readAttempts() ?: run { wipe(); return PinResult.TooMany }) + 1
        if (used > DataConstants.MAX_ATTEMPTS) { wipe(); return PinResult.TooMany }
        writeAttempts(used)

        val blob = blobFile.readBytes()
        val inner = try {
            require(blob.size > 1 + DataConstants.SALT_LEN + DataConstants.IV_LEN && blob[0] == DataConstants.VERSION)
            val key = loadKeystoreKey() ?: error("No key")
            decrypt(key, blob.copyOfRange(1 + DataConstants.SALT_LEN, blob.size))
        } catch (e: Exception) {
            e.printStackTrace()
            wipe()
            return PinResult.TooMany
        }

        val salt = blob.copyOfRange(1, 1 + DataConstants.SALT_LEN)
        val plain = try {
            decrypt(derive(pin, salt), inner)
        } catch (e: AEADBadTagException) {
            e.printStackTrace()
            val left = DataConstants.MAX_ATTEMPTS - used
            if (left <= 0) { wipe(); return PinResult.TooMany }
            return PinResult.Wrong(left)
        } finally {
            inner.fill(0)
        }

        writeAttempts(0)
        val chars = plain.toChars()
        plain.fill(0)
        return PinResult.Success(chars)
    }

    private fun derive(pin: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin, salt, DataConstants.PBKDF2_ITERATIONS, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun encrypt(key: SecretKey, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv + c.doFinal(data)
    }

    private fun decrypt(key: SecretKey, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data, 0, DataConstants.IV_LEN))
        return c.doFinal(data, DataConstants.IV_LEN, data.size - DataConstants.IV_LEN)
    }

    private fun createKeystoreKey(): SecretKey {
        val ks = keyStore()
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, DataConstants.ANDROID_KEY_STORE)
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun loadKeystoreKey(): SecretKey? = keyStore().getKey(alias, null) as? SecretKey

    private fun readAttempts(): Int? = metaFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()

    private fun writeAttempts(n: Int) = writeAtomic(metaFile, n.toString().toByteArray())
}