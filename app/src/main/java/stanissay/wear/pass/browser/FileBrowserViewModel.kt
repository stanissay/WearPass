package stanissay.wear.pass.browser

import android.app.Application
import android.content.ClipData
import android.os.Environment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.DataConstants
import java.io.File

class FileBrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = (app as App).vault
    private val root: File = Environment.getExternalStorageDirectory()
    private val _state = MutableStateFlow(BrowserUiState(dir = root))
    val state = _state.asStateFlow()
    val canGoUp get() = _state.value.dir != root

    fun refresh() {
        val access = Environment.isExternalStorageManager()
        val dir = _state.value.dir
        val list = if (access) {
            dir.listFiles().orEmpty()
                .filter { it.isDirectory || it.extension.equals(DataConstants.KDBX, ignoreCase = true) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } else emptyList()

        _state.update {
            it.copy(
                items = list,
                mode = when {
                    !access -> BrowserMode.NoAccess
                    it.mode == BrowserMode.NoAccess -> BrowserMode.Idle
                    else -> it.mode
                }
            )
        }
    }

    fun open(dir: File) {
        if (_state.value.mode == BrowserMode.Importing) return
        _state.update {
            it.copy(
                dir = dir,
                mode = if (it.mode is BrowserMode.Failed) BrowserMode.Idle else it.mode
            )
        }
        refresh()
    }

    fun up() { _state.value.dir.parentFile?.let { open(it) } }

    fun import(file: File, onDone: () -> Unit) {
        _state.update { it.copy(mode = BrowserMode.Importing) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = vault.importFrom(file)
            _state.update {
                it.copy(
                    mode = result.fold(
                        onSuccess = { BrowserMode.Idle },
                        onFailure = { e ->
                            BrowserMode.Failed(e.message ?: application.getString(R.string.error))
                        }
                    )
                )
            }
            if (result.isSuccess) withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun copyText(clipboard: Clipboard, text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", text)))
        }
    }

    init { refresh() }
}