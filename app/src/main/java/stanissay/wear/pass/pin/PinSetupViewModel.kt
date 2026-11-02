package stanissay.wear.pass.pin

import android.app.Application
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

class PinSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val vault = app.vault
    private val session = app.session
    private val pinVault = app.pinVault
    private val _state = MutableStateFlow(PinSetupState())
    val state = _state.asStateFlow()
    private var first: CharArray? = null

    fun onPin(pin: CharArray, onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return

        if (!s.confirming) {
            first = pin
            _state.update { it.copy(confirming = true, error = null, resetKey = it.resetKey + 1) }
            return
        }
        if (!pin.contentEquals(first)) {
            first = null
            _state.update { PinSetupState(error = application.getString(R.string.incorrect_pin), resetKey = it.resetKey + 1) }
            return
        }

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val password = session.consumePassword()
            val result = runCatching {
                pinVault.setup(pin, requireNotNull(password) { application.getString(R.string.session_expired) })
            }
            password?.fill('\u0000'); pin.fill('\u0000')
            first?.fill('\u0000'); first = null
            result.fold(
                onSuccess = {
                    vault.refresh()
                    withContext(Dispatchers.Main) { onDone() }
                },
                onFailure = { e ->
                    _state.update {
                        PinSetupState(error = e.message ?: application.getString(R.string.error), resetKey = it.resetKey + 1)
                    }
                }
            )
        }
    }
}