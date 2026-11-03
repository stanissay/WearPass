package stanissay.wear.pass.vault

import app.keemobile.kotpass.database.KeePassDatabase

class VaultSession {
    @Volatile var index: VaultIndex? = null
        private set
    var lastSync: SyncNote = SyncNote.None
        private set
    private var pendingPassword: CharArray? = null
    val isUnlocked get() = index != null

    fun open(db: KeePassDatabase, password: CharArray? = null, sync: SyncNote = SyncNote.None) {
        index = VaultIndex(db)
        pendingPassword = password
        lastSync = sync
    }

    fun lock() {
        index = null
        lastSync = SyncNote.None
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }

    fun consumePassword(): CharArray? = pendingPassword.also { pendingPassword = null }
}