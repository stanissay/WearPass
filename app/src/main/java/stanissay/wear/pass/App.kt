package stanissay.wear.pass

import android.app.Application
import stanissay.wear.pass.pin.KeystorePinVault
import stanissay.wear.pass.pin.PinVault
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.sl.SystemLockVault
import stanissay.wear.pass.utils.AutoLock
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.vault.BuiltInIcons
import stanissay.wear.pass.vault.VaultRepository
import stanissay.wear.pass.vault.VaultSession

class App : Application() {
    val autoLock by lazy { AutoLock { session.lock() } }
    val vault by lazy { VaultRepository(this) }
    val session = VaultSession()
    val pinVault: PinVault by lazy { KeystorePinVault(vault.pinBlobFile, vault.pinMetaFile, DataConstants.PIN_ALIAS) }
    val safeVault: PinVault by lazy { KeystorePinVault(vault.safeBlobFile, vault.safeMetaFile, DataConstants.SAFE_ALIAS) }
    val systemVault by lazy { SystemLockVault(vault.systemBlobFile) }
    val builtInIcons by lazy { BuiltInIcons(this) }

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