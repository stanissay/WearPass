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
import stanissay.wear.pass.utils.UnlockMethod
import stanissay.wear.pass.utils.PinResult
import stanissay.wear.pass.utils.PinUnlockState

class PinUnlockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val vault = app.vault
    private val session = app.session
    private val _state = MutableStateFlow(PinUnlockState())
    val state = _state.asStateFlow()

    fun onPin(method: UnlockMethod, pin: CharArray, onUnlocked: () -> Unit, onForcedMaster: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { app.codeVault(method).unlock(pin) }
            pin.fill('\u0000')
            result.fold(
                onSuccess = { r ->
                    when (r) {
                        is PinResult.Success -> {
                            val opened = runCatching { vault.openSyncing(String(r.password)) }
                            r.password.fill('\u0000')
                            opened.fold(
                                onSuccess = { o ->
                                    session.open(o.db, sync = o.sync)
                                    withContext(Dispatchers.Main) { onUnlocked() }
                                },
                                onFailure = { e ->
                                    app.codeVault(method).wipe()
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

    private fun fail(message: String) = _state.update { it.copy(busy = false, error = message, resetKey = it.resetKey + 1) }
}