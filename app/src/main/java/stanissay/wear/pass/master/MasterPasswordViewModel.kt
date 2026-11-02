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

sealed interface UnlockState {
    data object Idle : UnlockState
    data object Working : UnlockState
    data class Error(val message: String) : UnlockState
}

class MasterPasswordViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = (app as App).vault
    private val session = (app as App).session
    private val _state = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val state = _state.asStateFlow()

    fun unlock(password: String, onOpened: () -> Unit) {
        if (password.isEmpty() || _state.value is UnlockState.Working) return
        _state.value = UnlockState.Working
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { vault.openDatabase(password) }.fold(
                onSuccess = { db ->
                    session.open(db, password.toCharArray())
                    _state.value = UnlockState.Idle
                    withContext(Dispatchers.Main) { onOpened() }
                },
                onFailure = { e ->
                    _state.value = UnlockState.Error(
                        e.message ?: e::class.simpleName ?: application.getString(R.string.error)
                    )
                },
            )
        }
    }
}