package stanissay.wear.pass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PinSetupState(
    val confirming: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0,
)

class PinSetupViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = (app as App).vault
    private val session = (app as App).session
    private val pinVault = (app as App).pinVault

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
            _state.update { PinSetupState(error = "PIN не збігаються", resetKey = it.resetKey + 1) }
            return
        }

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val password = session.consumePassword()
            val result = runCatching {
                pinVault.setup(pin, requireNotNull(password) { "Сесія втрачена, введи пароль знову" })
            }
            password?.fill('\u0000')
            first?.fill('\u0000'); first = null
            result.fold(
                onSuccess = {
                    vault.refresh()
                    withContext(Dispatchers.Main) { onDone() }
                },
                onFailure = { e ->
                    _state.update {
                        PinSetupState(error = e.message ?: "Не вдалося зберегти PIN", resetKey = it.resetKey + 1)
                    }
                },
            )
        }
    }
}