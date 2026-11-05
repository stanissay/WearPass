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
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.SystemSetupState
import stanissay.wear.pass.utils.UnlockMethod

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
                onFailure = { e -> _state.value =
                    SystemSetupState(error = e.message ?: application.getString(R.string.error))
                }
            )
        }
    }
}