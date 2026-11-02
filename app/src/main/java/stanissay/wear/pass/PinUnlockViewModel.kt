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

data class PinUnlockState(
    val busy: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0,
)

class PinUnlockViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = (app as App).vault
    private val session = (app as App).session
    private val pinVault = (app as App).pinVault

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
                                onFailure = { e -> fail(e.message ?: "Не вдалося відкрити базу") },
                            )
                        }
                        is PinResult.Wrong -> fail("Невірний PIN. Спроб: ${r.attemptsLeft}")
                        PinResult.TooMany -> {
                            vault.refresh() // блок стерто, статус стає NeedsMasterPassword
                            withContext(Dispatchers.Main) { onForcedMaster() }
                        }
                    }
                },
                onFailure = { e -> fail(e.message ?: "Помилка розблокування") },
            )
        }
    }

    private fun fail(message: String) =
        _state.update { it.copy(busy = false, error = message, resetKey = it.resetKey + 1) }
}