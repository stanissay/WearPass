package stanissay.wear.pass.sl

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stanissay.wear.pass.utils.toChars
import stanissay.wear.pass.utils.toUtf8
import stanissay.wear.pass.utils.writeAtomic
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
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
            writeAtomic(blobFile, byteArrayOf(VERSION) + c.iv + c.doFinal(plain))
        } finally {
            plain.fill(0)
        }
    }

    suspend fun unlock(): SystemUnlockResult = withContext(Dispatchers.Default) {
        if (!blobFile.exists()) return@withContext SystemUnlockResult.Invalidated
        try {
            val blob = blobFile.readBytes()
            val key = keyStore().getKey(ALIAS, null) as? SecretKey ?: run { wipe(); return@withContext SystemUnlockResult.Invalidated }
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 1, IV_LEN))
            val plain = c.doFinal(blob, 1 + IV_LEN, blob.size - 1 - IV_LEN)
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
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun createKey(): SecretKey {
        val ks = keyStore()
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUnlockedDeviceRequired(true)
                .build()
        )
        return gen.generateKey()
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private companion object {
        const val ALIAS = "wearkeepass_system_lock"
        const val VERSION: Byte = 1
        const val IV_LEN = 12
    }
}