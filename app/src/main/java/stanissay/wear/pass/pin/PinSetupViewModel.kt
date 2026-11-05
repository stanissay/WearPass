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
import stanissay.wear.pass.utils.PinSetupState

class PinSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val session = app.session
    private val _state = MutableStateFlow(PinSetupState())
    val state = _state.asStateFlow()
    private var first: CharArray? = null

    fun onPin(method: UnlockMethod, pin: CharArray, onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return

        if (!s.confirming) {
            first = pin
            _state.update { it.copy(confirming = true, error = null, resetKey = it.resetKey + 1) }
            return
        }
        if (!pin.contentEquals(first)) {
            first = null
            _state.update {
                PinSetupState(
                    error = application.getString(R.string.incorrect_pin),
                    resetKey = it.resetKey + 1
                )
            }
            return
        }

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val password = session.consumePassword()
            val result = runCatching {
                app.codeVault(method).setup(pin, requireNotNull(password) { application.getString(R.string.session_expired) })
            }
            password?.fill('\u0000'); pin.fill('\u0000')
            first?.fill('\u0000'); first = null
            result.fold(
                onSuccess = {
                    app.commitMethod(method)
                    withContext(Dispatchers.Main) { onDone() }
                },
                onFailure = { e ->
                    _state.update {
                        PinSetupState(
                            error = e.message ?: application.getString(R.string.error),
                            resetKey = it.resetKey + 1
                        )
                    }
                }
            )
        }
    }
}