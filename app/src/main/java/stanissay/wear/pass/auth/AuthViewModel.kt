package stanissay.wear.pass.auth

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
import stanissay.wear.pass.pin.PinResult
import stanissay.wear.pass.sl.SystemUnlockResult

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    val method = app.vault.unlockMethod
    private val _state = MutableStateFlow(ReauthState())
    val state = _state.asStateFlow()

    fun confirmPin(pin: CharArray, onConfirmed: () -> Unit, onForcedMaster: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { app.pinVault.unlock(pin) }
            pin.fill('\u0000')
            result.fold(
                onSuccess = { r ->
                    when (r) {
                        is PinResult.Success -> {
                            app.session.stashPassword(r.password)
                            withContext(Dispatchers.Main) { onConfirmed() }
                        }
                        is PinResult.Wrong -> _state.update {
                            it.copy(busy = false, message = application.getString(R.string.attempts), resetKey = it.resetKey + 1)
                        }
                        PinResult.TooMany -> {
                            app.session.lock(); app.vault.refresh()
                            withContext(Dispatchers.Main) { onForcedMaster() }
                        }
                    }
                },
                onFailure = { e -> _state.update { it.copy(busy = false, message = e.message, resetKey = it.resetKey + 1) } },
            )
        }
    }

    fun confirmSystem(onConfirmed: () -> Unit, onForcedMaster: () -> Unit) {
        if (_state.value.busy) return
        _state.value = ReauthState(busy = true)
        viewModelScope.launch(Dispatchers.Default) {
            when (val r = app.systemVault.unlock()) {
                is SystemUnlockResult.Success -> {
                    app.session.stashPassword(r.password)
                    withContext(Dispatchers.Main) { onConfirmed() }
                }
                SystemUnlockResult.DeviceLocked -> _state.value = ReauthState(locked = true)
                SystemUnlockResult.Invalidated -> {
                    app.session.lock(); app.vault.refresh()
                    withContext(Dispatchers.Main) { onForcedMaster() }
                }
                is SystemUnlockResult.Failed -> _state.value = ReauthState(message = r.message)
            }
        }
    }
}