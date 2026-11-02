package stanissay.wear.pass.vault

import android.content.Context
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.DataConstants
import java.io.DataInputStream
import java.io.File

class VaultRepository(private val context: Context) {
    private val dir get() = File(context.filesDir, "vault").apply { mkdirs() }
    val dbFile get() = File(dir, "db.kdbx")
    private val _status = MutableStateFlow(compute())
    val status = _status.asStateFlow()
    val pinBlobFile get() = File(dir, "pin.blob")
    val pinMetaFile get() = File(dir, "pin.meta")

    private fun compute() = when {
        !dbFile.exists() -> VaultStatus.NotImported
        !pinBlobFile.exists() -> VaultStatus.NeedsMasterPassword
        else -> VaultStatus.PinLocked
    }

    fun refresh() { _status.value = compute() }

    fun importFrom(source: File): Result<Unit> = runCatching {
        require(source.length() in 1..DataConstants.KDBX_MAX_SIZE) { context.getString(R.string.incorrect_file) }
        val tmp = File(dir, "db.kdbx.tmp")
        try {
            source.inputStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            check(hasKdbxSignature(tmp)) { context.getString(R.string.incorrect_file) }
            pinBlobFile.delete()
            pinMetaFile.delete()
            check(tmp.renameTo(dbFile)) { context.getString(R.string.error) }
        } finally {
            tmp.delete()
        }
        refresh()
    }

    fun openDatabase(password: String): KeePassDatabase {
        val credentials = Credentials.from(EncryptedValue.fromString(password))
        return dbFile.inputStream().use { KeePassDatabase.decode(it, credentials) }
    }

    fun wipeFiles() {
        dir.deleteRecursively()
        refresh()
    }

    private fun hasKdbxSignature(f: File): Boolean = runCatching {
        val head = ByteArray(8)
        DataInputStream(f.inputStream()).use { it.readFully(head) }
        head.contentEquals(DataConstants.KDBX_SIGNATURE)
    }.getOrDefault(false)
}