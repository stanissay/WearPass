package stanissay.wear.pass.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
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

data class Revealed(val id: String, val value: String)

@SuppressLint("WearRecents")
fun openCredentialSettings(context: Context) {
    runCatching { androidx.credentials.CredentialManager.create(context).createSettingsPendingIntent().send() }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent("android.settings.CREDENTIAL_PROVIDER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}

class SwipeGate {
    private var blockers by mutableIntStateOf(0)
    val enabled get() = blockers == 0
    fun block() { blockers++ }
    fun unblock() { blockers = (blockers - 1).coerceAtLeast(0) }
}

class RotaryBus {
    @Volatile var listener: ((Float) -> Boolean)? = null
}