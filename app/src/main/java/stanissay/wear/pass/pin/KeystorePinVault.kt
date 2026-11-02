package stanissay.wear.pass.pin

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stanissay.wear.pass.utils.toChars
import stanissay.wear.pass.utils.toUtf8
import stanissay.wear.pass.utils.writeAtomic
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeystorePinVault(
    private val blobFile: File,
    private val metaFile: File,
) : PinVault {

    override suspend fun setup(pin: CharArray, password: CharArray) =
        withContext(Dispatchers.Default) { doSetup(pin, password) }

    override suspend fun unlock(pin: CharArray): PinResult =
        withContext(Dispatchers.Default) { doUnlock(pin) }

    override fun wipe() {
        blobFile.delete()
        metaFile.delete()
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun doSetup(pin: CharArray, password: CharArray) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val plain = password.toUtf8()
        try {
            val inner = encrypt(derive(pin, salt), plain)
            val outer = encrypt(createKeystoreKey(), inner)
            writeAtomic(blobFile, byteArrayOf(VERSION) + salt + outer)
            writeAttempts(0)
        } finally {
            plain.fill(0)
        }
    }

    private fun doUnlock(pin: CharArray): PinResult {
        if (!blobFile.exists()) return PinResult.TooMany

        val used = (readAttempts() ?: run { wipe(); return PinResult.TooMany }) + 1
        if (used > MAX_ATTEMPTS) { wipe(); return PinResult.TooMany }
        writeAttempts(used)

        val blob = blobFile.readBytes()
        val inner = try {
            require(blob.size > 1 + SALT_LEN + IV_LEN && blob[0] == VERSION)
            val key = loadKeystoreKey() ?: error("Ключ Keystore відсутній")
            decrypt(key, blob.copyOfRange(1 + SALT_LEN, blob.size))
        } catch (e: Exception) {
            e.printStackTrace()
            wipe()
            return PinResult.TooMany
        }

        val salt = blob.copyOfRange(1, 1 + SALT_LEN)
        val plain = try {
            decrypt(derive(pin, salt), inner)
        } catch (e: AEADBadTagException) {
            e.printStackTrace()
            val left = MAX_ATTEMPTS - used
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
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, 256)
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
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data, 0, IV_LEN))
        return c.doFinal(data, IV_LEN, data.size - IV_LEN)
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun createKeystoreKey(): SecretKey {
        val ks = keyStore()
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun loadKeystoreKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun readAttempts(): Int? = metaFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()

    private fun writeAttempts(n: Int) = writeAtomic(metaFile, n.toString().toByteArray())

    private companion object {
        const val ALIAS = "wearkeepass_pin_wrap"
        const val VERSION: Byte = 1
        const val SALT_LEN = 16
        const val IV_LEN = 12
        const val MAX_ATTEMPTS = 5
        const val PBKDF2_ITERATIONS = 200_000
    }
}