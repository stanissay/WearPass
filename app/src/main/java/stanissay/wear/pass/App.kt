package stanissay.wear.pass

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import stanissay.wear.pass.pin.KeystorePinVault
import stanissay.wear.pass.pin.PinVault
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.sl.SystemLockVault
import stanissay.wear.pass.utils.AutoLock
import stanissay.wear.pass.utils.BodyLock
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.RotaryBus
import stanissay.wear.pass.utils.SwipeGate
import stanissay.wear.pass.vault.BuiltInIcons
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