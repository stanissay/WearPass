package stanissay.wear.pass.utils

import java.io.File

enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked }

sealed interface BrowserMode {
    data object NoAccess : BrowserMode
    data object Idle : BrowserMode
    data object Importing : BrowserMode
    data class Failed(val message: String) : BrowserMode
}

data class BrowserUiState(
    val dir: File,
    val items: List<File> = emptyList(),
    val mode: BrowserMode = BrowserMode.Idle
)

data class PinSetupState(
    val confirming: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)

data class PinUnlockState(
    val busy: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)

object DataConstants {
    const val KDBX = "kdbx"
    const val KDBX_MAX_SIZE = 64L * 1024 * 1024
    val KDBX_SIGNATURE = byteArrayOf(
        0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte(),
        0x67, 0xFB.toByte(), 0x4B, 0xB5.toByte()
    )
    const val BROWSER_UP = ".."
    const val PIN_LENGTH = 4
}