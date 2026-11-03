package stanissay.wear.pass.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.NavKey
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.MessageDigest

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

object AppEventManager {
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val uiEvents = _uiEvents.asSharedFlow()

    fun sendEvent(event: UiEvent) {
        managerScope.launch {
            _uiEvents.emit(event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetReplayCache() {
        _uiEvents.resetReplayCache()
    }
}

fun showCustomToast(text: String, context: Context) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

@SuppressLint("ModifierFactoryExtensionFunction")
fun TransformingLazyColumnItemScope.transformedItem(
    transformationSpec: TransformationSpec
): Modifier =
    Modifier
        .transformedHeight(this, transformationSpec)
        .graphicsLayer {
            with(transformationSpec) {
                applyContainerTransformation(scrollProgress)
            }
        }

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