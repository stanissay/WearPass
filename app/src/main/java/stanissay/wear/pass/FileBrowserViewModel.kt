package stanissay.wear.pass

import android.app.Application
import android.content.ClipData
import android.os.Environment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.utils.BrowserUiState
import java.io.File

class FileBrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = (app as App).vault
    private val root: File = Environment.getExternalStorageDirectory()
    private val _state = MutableStateFlow(BrowserUiState(dir = root))
    val state = _state.asStateFlow()
    val canGoUp get() = _state.value.dir != root

    fun refresh() {
        val s = _state.value
        val access = Environment.isExternalStorageManager()
        val list = if (access) {
            s.dir.listFiles().orEmpty()
                .filter { it.isDirectory || it.extension.equals("kdbx", true) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } else emptyList()
        _state.update { it.copy(items = list, hasAccess = access) }
    }

    fun open(dir: File) { _state.update { it.copy(dir = dir, error = null) }; refresh() }

    fun up() { _state.value.dir.parentFile?.let { open(it) } }

    fun import(file: File, onDone: () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val r = vault.importFrom(file)
            _state.update { it.copy(busy = false, error = r.exceptionOrNull()?.message) }
            if (r.isSuccess) withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun copyText(clipboard: Clipboard, text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", text)))
        }
    }

    init { refresh() }
}