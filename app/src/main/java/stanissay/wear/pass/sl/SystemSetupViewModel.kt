package stanissay.wear.pass.sl

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
import stanissay.wear.pass.settings.UnlockMethod

class SystemSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val _state = MutableStateFlow(SystemSetupState())
    val state = _state.asStateFlow()

    fun enable(onDone: () -> Unit) {
        if (_state.value.busy) return
        _state.value = SystemSetupState(busy = true)
        viewModelScope.launch(Dispatchers.Default) {
            val password = app.session.consumePassword()
            val result = runCatching {
                requireNotNull(password) { application.getString(R.string.session_expired) }
                app.systemVault.setup(password)
                app.commitMethod(UnlockMethod.SystemLock)
            }
            password?.fill('\u0000')
            result.fold(
                onSuccess = { withContext(Dispatchers.Main) { onDone() } },
                onFailure = { e -> _state.value = SystemSetupState(error = e.message ?: application.getString(R.string.error)) }
            )
        }
    }
}