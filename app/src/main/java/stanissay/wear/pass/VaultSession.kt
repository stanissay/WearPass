package stanissay.wear.pass

import app.keemobile.kotpass.database.KeePassDatabase

class VaultSession {
    @Volatile var database: KeePassDatabase? = null
        private set
    private var pendingPassword: CharArray? = null

    val isUnlocked get() = database != null

    fun open(db: KeePassDatabase, password: CharArray? = null) {
        database = db
        pendingPassword = password
    }

    /** Віддає пароль один раз для PinSetup. Викликач має обнулити масив після використання. */
    fun consumePassword(): CharArray? = pendingPassword.also { pendingPassword = null }

    fun lock() {
        database = null
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }
}