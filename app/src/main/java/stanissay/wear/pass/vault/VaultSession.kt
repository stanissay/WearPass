/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass.vault

import app.keemobile.kotpass.database.KeePassDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import stanissay.wear.pass.utils.SyncNote

class VaultSession {
    @Volatile var index: VaultIndex? = null
        private set
    var lastSync: SyncNote = SyncNote.None
        private set
    private var pendingPassword: CharArray? = null
    val isUnlocked get() = index != null
    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()

    fun open(db: KeePassDatabase, password: CharArray? = null, sync: SyncNote = SyncNote.None) {
        index = VaultIndex(db)
        pendingPassword = password
        lastSync = sync
        _unlocked.value = true
    }

    fun lock() {
        index = null
        lastSync = SyncNote.None
        pendingPassword?.fill('\u0000')
        pendingPassword = null
        _unlocked.value = false
    }

    fun consumePassword(): CharArray? = pendingPassword.also { pendingPassword = null }

    fun clearPendingPassword() {
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }

    fun stashPassword(password: CharArray) {
        pendingPassword?.fill('\u0000')
        pendingPassword = password
    }
}