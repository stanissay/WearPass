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

package stanissay.wear.pass.master

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
import stanissay.wear.pass.utils.UnlockState

class MasterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<App>()
    private val vault = app.vault
    private val session = app.session
    private val _state = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val state = _state.asStateFlow()

    fun unlock(password: String, onOpened: () -> Unit) {
        if (password.isEmpty() || _state.value is UnlockState.Working) return
        _state.value = UnlockState.Working
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { vault.openSyncing(password) }.fold(
                onSuccess = { r ->
                    session.open(r.db, password.toCharArray(), r.sync)
                    withContext(Dispatchers.Main) { onOpened() }
                },
                onFailure = { e ->
                    _state.value = UnlockState.Error(e.message ?: e::class.simpleName ?: application.getString(R.string.error))
                }
            )
        }
    }
}