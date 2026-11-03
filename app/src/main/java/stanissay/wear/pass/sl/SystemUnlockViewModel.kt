package stanissay.wear.pass.sl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App

class SystemUnlockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val _state = MutableStateFlow(SystemUnlockState())
    val state = _state.asStateFlow()
    private var running = false

    fun tryUnlock(onUnlocked: () -> Unit, onForcedMaster: () -> Unit) {
        if (running) return
        running = true
        _state.value = SystemUnlockState(busy = true)
        viewModelScope.launch(Dispatchers.Default) {
            try {
                when (val r = app.systemVault.unlock()) {
                    is SystemUnlockResult.Success -> {
                        val opened = runCatching { app.vault.openSyncing(String(r.password)) }
                        r.password.fill('\u0000')
                        opened.fold(
                            onSuccess = { o ->
                                app.session.open(o.db, sync = o.sync)
                                withContext(Dispatchers.Main) { onUnlocked() }
                            },
                            onFailure = {
                                app.systemVault.wipe(); app.vault.refresh()
                                withContext(Dispatchers.Main) { onForcedMaster() }
                            },
                        )
                    }
                    SystemUnlockResult.DeviceLocked ->
                        _state.value = SystemUnlockState(busy = false, locked = true)
                    SystemUnlockResult.Invalidated -> {
                        app.vault.refresh()
                        withContext(Dispatchers.Main) { onForcedMaster() }
                    }
                    is SystemUnlockResult.Failed ->
                        _state.value = SystemUnlockState(busy = false, message = r.message)
                }
            } finally { running = false }
        }
    }
}