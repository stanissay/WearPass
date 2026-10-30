package stanissay.wear.pass.utils

import java.io.File

enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked }

data class BrowserUiState(
    val dir: File,
    val items: List<File> = emptyList(),
    val hasAccess: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
)