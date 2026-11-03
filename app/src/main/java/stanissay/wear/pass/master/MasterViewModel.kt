package stanissay.wear.pass.master

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.R

class MasterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val vault = app.vault
    private val session = app.session
    private val _state = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val state = _state.asStateFlow()

    fun unlock(password: String, onOpened: () -> Unit) {
        if (password.isEmpty() || _state.value is UnlockState.Working) return
        _state.value = UnlockState.Working
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { vault.openSyncing(password) }.fold(
                onSuccess = { r ->
                    session.open(r.db, password.toCharArray(), r.sync)
                    _state.value = UnlockState.Idle
                    withContext(Dispatchers.Main) { onOpened() }
                },
                onFailure = { e ->
                    _state.value = UnlockState.Error(e.message ?: e::class.simpleName ?: application.getString(R.string.error))
                }
            )
        }
    }
}