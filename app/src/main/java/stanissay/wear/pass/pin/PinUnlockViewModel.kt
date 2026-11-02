package stanissay.wear.pass.pin

import android.app.Application
import android.os.SystemClock
import android.util.Log
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

class PinUnlockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val vault = app.vault
    private val session = app.session
    private val pinVault = app.pinVault
    private val _state = MutableStateFlow(PinUnlockState())
    val state = _state.asStateFlow()

    fun onPin(pin: CharArray, onUnlocked: () -> Unit, onForcedMaster: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { pinVault.unlock(pin) }
            pin.fill('\u0000')
            result.fold(
                onSuccess = { r ->
                    when (r) {
                        is PinResult.Success -> {
                            val opened = runCatching { vault.openDatabase(String(r.password)) }
                            r.password.fill('\u0000')
                            opened.fold(
                                onSuccess = { db ->
                                    session.open(db)
                                    _state.update { it.copy(busy = false) }
                                    withContext(Dispatchers.Main) { onUnlocked() }
                                },
                                onFailure = { e ->
                                    pinVault.wipe()
                                    vault.refresh()
                                    onForcedMaster()
                                    fail(e.message ?: application.getString(R.string.error))
                                },
                            )
                        }
                        is PinResult.Wrong -> fail("${application.getString(R.string.attempts)} ${r.attemptsLeft}")
                        PinResult.TooMany -> {
                            vault.refresh()
                            withContext(Dispatchers.Main) { onForcedMaster() }
                        }
                    }
                },
                onFailure = { e -> fail(e.message ?: application.getString(R.string.error)) },
            )
        }
    }

    private fun fail(message: String) =
        _state.update { it.copy(busy = false, error = message, resetKey = it.resetKey + 1) }
}