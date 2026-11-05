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

package stanissay.wear.pass.lock

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.SystemUnlockResult
import stanissay.wear.pass.utils.keyStore
import stanissay.wear.pass.utils.toChars
import stanissay.wear.pass.utils.toUtf8
import stanissay.wear.pass.utils.writeAtomic
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SystemLockVault(private val blobFile: File) {
    fun isAvailable(context: Context): Boolean = context.getSystemService(KeyguardManager::class.java).isDeviceSecure

    suspend fun setup(password: CharArray) = withContext(Dispatchers.Default) {
        val plain = password.toUtf8()
        try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, createKey())
            writeAtomic(blobFile, byteArrayOf(DataConstants.VERSION) + c.iv + c.doFinal(plain))
        } finally {
            plain.fill(0)
        }
    }

    suspend fun unlock(): SystemUnlockResult = withContext(Dispatchers.Default) {
        if (!blobFile.exists()) return@withContext SystemUnlockResult.Invalidated
        try {
            val blob = blobFile.readBytes()
            val key = keyStore().getKey(DataConstants.SYS_ALIAS, null) as? SecretKey ?: run { wipe(); return@withContext SystemUnlockResult.Invalidated }
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 1, DataConstants.IV_LEN))
            val plain = c.doFinal(blob, 1 + DataConstants.IV_LEN, blob.size - 1 - DataConstants.IV_LEN)
            val chars = plain.toChars()
            plain.fill(0)
            SystemUnlockResult.Success(chars)
        } catch (e: UserNotAuthenticatedException) {
            e.printStackTrace()
            SystemUnlockResult.DeviceLocked
        } catch (e: KeyPermanentlyInvalidatedException) {
            e.printStackTrace()
            wipe(); SystemUnlockResult.Invalidated
        } catch (e: AEADBadTagException) {
            e.printStackTrace()
            wipe(); SystemUnlockResult.Invalidated
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            SystemUnlockResult.Failed(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    fun wipe() {
        blobFile.delete()
        runCatching { keyStore().deleteEntry(DataConstants.SYS_ALIAS) }
    }

    private fun createKey(): SecretKey {
        val ks = keyStore()
        if (ks.containsAlias(DataConstants.SYS_ALIAS)) ks.deleteEntry(DataConstants.SYS_ALIAS)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, DataConstants.ANDROID_KEY_STORE)
        gen.init(
            KeyGenParameterSpec.Builder(DataConstants.SYS_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUnlockedDeviceRequired(true)
                .build()
        )
        return gen.generateKey()
    }
}