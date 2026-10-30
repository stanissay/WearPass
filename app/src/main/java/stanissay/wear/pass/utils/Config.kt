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
    val mode: BrowserMode = BrowserMode.Idle,
)