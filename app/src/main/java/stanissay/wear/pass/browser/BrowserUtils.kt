package stanissay.wear.pass.browser

import java.io.File

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