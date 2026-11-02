package stanissay.wear.pass

import android.app.Application
import stanissay.wear.pass.pin.KeystorePinVault
import stanissay.wear.pass.pin.PinVault
import stanissay.wear.pass.vault.VaultRepository
import stanissay.wear.pass.vault.VaultSession

class App : Application() {
    val vault by lazy { VaultRepository(this) }
    val session = VaultSession()
    val pinVault: PinVault by lazy { KeystorePinVault(vault.pinBlobFile, vault.pinMetaFile) }

    fun wipeAll() {
        session.lock()
        pinVault.wipe()
        vault.wipe()
    }
}