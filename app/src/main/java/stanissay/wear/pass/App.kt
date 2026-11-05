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

package stanissay.wear.pass

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import stanissay.wear.pass.pin.KeystorePinVault
import stanissay.wear.pass.utils.PinVault
import stanissay.wear.pass.utils.UnlockMethod
import stanissay.wear.pass.lock.SystemLockVault
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.RotaryBus
import stanissay.wear.pass.utils.SwipeGate
import stanissay.wear.pass.utils.BuiltInIcons
import stanissay.wear.pass.vault.VaultRepository
import stanissay.wear.pass.vault.VaultSession

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val bodyLock by lazy { BodyLock(this) { session.lock() } }
    val autoLock by lazy { AutoLock { session.lock() } }
    val vault by lazy { VaultRepository(this) }
    val session = VaultSession()
    val pinVault: PinVault by lazy { KeystorePinVault(vault.pinBlobFile, vault.pinMetaFile, DataConstants.PIN_ALIAS) }
    val safeVault: PinVault by lazy { KeystorePinVault(vault.safeBlobFile, vault.safeMetaFile, DataConstants.SAFE_ALIAS) }
    val systemVault by lazy { SystemLockVault(vault.systemBlobFile) }
    val builtInIcons by lazy { BuiltInIcons(this) }
    val swipeGate = SwipeGate()
    val rotaryBus = RotaryBus()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            session.unlocked.collect { unlocked ->
                if (unlocked) bodyLock.start() else bodyLock.stop()
            }
        }
    }

    fun codeVault(method: UnlockMethod): PinVault = if (method == UnlockMethod.Pin) pinVault else safeVault

    fun commitMethod(method: UnlockMethod) {
        vault.setUnlockMethod(method)
        if (method != UnlockMethod.Pin) pinVault.wipe()
        if (method != UnlockMethod.Safe) safeVault.wipe()
        if (method != UnlockMethod.SystemLock) systemVault.wipe()
        vault.refresh()
    }

    fun wipeUnlockSecrets() { pinVault.wipe(); safeVault.wipe(); systemVault.wipe() }

    fun wipeAll() {
        session.lock()
        wipeUnlockSecrets()
        vault.wipeFiles()
    }
}