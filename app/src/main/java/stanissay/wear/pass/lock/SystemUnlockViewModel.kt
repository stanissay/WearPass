/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.utils.SystemUnlockResult
import stanissay.wear.pass.utils.SystemUnlockState

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