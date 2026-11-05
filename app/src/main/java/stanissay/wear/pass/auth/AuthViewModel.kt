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
import stanissay.wear.pass.utils.PinResult
import stanissay.wear.pass.utils.SystemUnlockResult
import stanissay.wear.pass.utils.AuthState

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    val method get() = app.vault.unlockMethod
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun confirmPin(pin: CharArray, onConfirmed: () -> Unit, onForcedMaster: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { app.codeVault(method).unlock(pin) }
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
        _state.value = AuthState(busy = true)
        viewModelScope.launch(Dispatchers.Default) {
            when (val r = app.systemVault.unlock()) {
                is SystemUnlockResult.Success -> {
                    app.session.stashPassword(r.password)
                    withContext(Dispatchers.Main) { onConfirmed() }
                }
                SystemUnlockResult.DeviceLocked -> _state.value = AuthState(locked = true)
                SystemUnlockResult.Invalidated -> {
                    app.session.lock(); app.vault.refresh()
                    withContext(Dispatchers.Main) { onForcedMaster() }
                }
                is SystemUnlockResult.Failed -> _state.value = AuthState(message = r.message)
            }
        }
    }
}