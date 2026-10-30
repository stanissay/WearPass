package stanissay.wear.pass

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import stanissay.wear.pass.utils.VaultStatus
import java.io.File

class VaultRepository(private val context: Context) {

    private val dir get() = File(context.filesDir, "vault").apply { mkdirs() }
    val dbFile get() = File(dir, "db.kdbx")
    private val pinBlob get() = File(dir, "pin.blob")

    private val _status = MutableStateFlow(compute())
    val status = _status.asStateFlow()

    private fun compute() = when {
        !dbFile.exists() -> VaultStatus.NotImported
        !pinBlob.exists() -> VaultStatus.NeedsMasterPassword
        else -> VaultStatus.PinLocked
    }

    fun refresh() { _status.value = compute() }

    /** Копіює у внутрішнє сховище. Стара база лишається, поки нова не пройшла перевірку. */
    fun importFrom(source: File): Result<Unit> = runCatching {
        require(source.length() in 1..MAX_SIZE) { "Недопустимий розмір файлу" }
        val tmp = File(dir, "db.kdbx.tmp")
        try {
            source.inputStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            check(hasKdbxSignature(tmp)) { "Це не файл KDBX" }
            check(tmp.renameTo(dbFile)) { "Не вдалося замінити базу" }
            pinBlob.delete() // нова база означає старий PIN-блок недійсний
        } finally {
            tmp.delete()
        }
        refresh()
    }

    fun wipe() {
        dir.deleteRecursively()
        // TODO: видалити ключ з Android Keystore і скинути лічильник спроб PIN
        refresh()
    }

    private fun hasKdbxSignature(f: File): Boolean {
        val head = ByteArray(8)
        val ok = f.inputStream().use { it.read(head) == 8 }
        return ok && head.contentEquals(KDBX_SIGNATURE)
    }

    private companion object {
        const val MAX_SIZE = 64L * 1024 * 1024
        val KDBX_SIGNATURE = byteArrayOf(
            0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte(),
            0x67, 0xFB.toByte(), 0x4B, 0xB5.toByte()
        )
    }
}