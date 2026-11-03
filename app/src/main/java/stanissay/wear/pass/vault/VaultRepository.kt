package stanissay.wear.pass.vault

import android.content.Context
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import stanissay.wear.pass.R
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.sha256
import stanissay.wear.pass.utils.writeAtomic
import java.io.DataInputStream
import java.io.File

class VaultRepository(private val context: Context) {
    private val dir get() = File(context.filesDir, "vault").apply { mkdirs() }
    val dbFile get() = File(dir, "db.kdbx")
    private val _status = MutableStateFlow(compute())
    val status = _status.asStateFlow()
    val pinBlobFile get() = File(dir, "pin.blob")
    val pinMetaFile get() = File(dir, "pin.meta")
    val systemBlobFile get() = File(dir, "system.blob")
    private val sourcePathFile get() = File(dir, "source.path")
    val sourcePath: String?
        get() = sourcePathFile.takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { null }
    private val methodFile get() = File(dir, "unlock.method")
    val unlockMethod: UnlockMethod
        get() = methodFile.takeIf { it.exists() }?.readText()?.trim()
            ?.let { n -> UnlockMethod.entries.firstOrNull { it.name == n } } ?: UnlockMethod.Pin

    fun setUnlockMethod(m: UnlockMethod) = writeAtomic(methodFile, m.name.toByteArray())

    private fun credentials(password: String) =
        Credentials.from(EncryptedValue.fromString(password))

    private fun compute(): VaultStatus {
        if (!dbFile.exists()) return VaultStatus.NotImported
        return when (unlockMethod) {
            UnlockMethod.Pin ->
                if (pinBlobFile.exists()) VaultStatus.PinLocked else VaultStatus.NeedsMasterPassword
            UnlockMethod.SystemLock ->
                if (systemBlobFile.exists()) VaultStatus.SystemLocked else VaultStatus.NeedsMasterPassword
            UnlockMethod.Safe -> VaultStatus.NeedsMasterPassword // TODO: коли буде сейф
        }
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
            systemBlobFile.delete()
            methodFile.delete()
            writeAtomic(sourcePathFile, source.absolutePath.toByteArray())
            check(tmp.renameTo(dbFile)) { context.getString(R.string.error) }
        } finally {
            tmp.delete()
        }
        refresh()
    }

    fun openDatabase(password: String): KeePassDatabase =
        dbFile.inputStream().use { KeePassDatabase.decode(it, credentials(password)) }

    fun openSyncing(password: String): OpenResult {
        val path = sourcePath ?: return OpenResult(openDatabase(password), SyncNote.None)
        val src = File(path)
        if (!src.isFile) return OpenResult(openDatabase(password), SyncNote.SourceMissing)

        return runCatching { syncFrom(src, password) }.fold(
            onSuccess = { s -> OpenResult(s.db ?: openDatabase(password), s.note) },
            onFailure = { OpenResult(openDatabase(password), SyncNote.SourceUnreadable) },
        )
    }

    private fun syncFrom(src: File, password: String): Synced {
        require(src.length() in 1..DataConstants.KDBX_MAX_SIZE)
        val tmp = File(dir, "db.kdbx.tmp")
        try {
            src.inputStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            check(hasKdbxSignature(tmp))
            if (sha256(tmp).contentEquals(sha256(dbFile))) return Synced(SyncNote.None, null)
            val db = tmp.inputStream().use { KeePassDatabase.decode(it, credentials(password)) }
            check(tmp.renameTo(dbFile))
            return Synced(SyncNote.Updated, db)
        } finally {
            tmp.delete()
        }
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