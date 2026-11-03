package stanissay.wear.pass.utils

import androidx.navigation3.runtime.NavKey
import stanissay.wear.pass.settings.UnlockMethod
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.MessageDigest

fun CharArray.toUtf8(): ByteArray {
    val bb = Charsets.UTF_8.encode(CharBuffer.wrap(this))
    val out = ByteArray(bb.remaining()).also { bb.get(it) }
    bb.array().fill(0)
    return out
}

fun ByteArray.toChars(): CharArray {
    val cb = Charsets.UTF_8.decode(ByteBuffer.wrap(this))
    return CharArray(cb.remaining()).also { cb.get(it) }
}

fun writeAtomic(target: File, bytes: ByteArray) {
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeBytes(bytes)
    check(tmp.renameTo(target)) { "Не вдалося записати ${target.name}" }
}

fun keyStore() = KeyStore.getInstance(DataConstants.ANDROID_KEY_STORE).apply { load(null) }!!

fun sha256(f: File): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    f.inputStream().use { i ->
        val buf = ByteArray(8192)
        while (true) {
            val n = i.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
        }
    }
    return md.digest()
}

fun NavKey.requiresUnlock() = this is GroupKey || this is EntryKey || this is AuthKey || this is PinSetupKey || this is SafeSetupKey || this is SystemSetupKey

fun codeLength(method: UnlockMethod) = if (method == UnlockMethod.Pin) DataConstants.PIN_LENGTH else DataConstants.SAFE_LENGTH

data class Revealed(val id: String, val value: String)