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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R
import stanissay.wear.pass.pin.PinPad
import stanissay.wear.pass.pin.SafeDial
import stanissay.wear.pass.utils.UnlockMethod
import stanissay.wear.pass.lock.SystemPrompt

@Composable
fun AuthScreen(
    onConfirmed: () -> Unit,
    onForcedMaster: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (viewModel.method) {
        UnlockMethod.Pin -> PinPad(
            title = stringResource(R.string.confirm_pin),
            subtitle = state.message,
            enabled = !state.busy,
            resetKey = state.resetKey,
            onComplete = { viewModel.confirmPin(it, onConfirmed, onForcedMaster) },
        )
        UnlockMethod.SystemLock -> {
            LifecycleResumeEffect(Unit) {
                viewModel.confirmSystem(onConfirmed, onForcedMaster)
                onPauseOrDispose { }
            }
            SystemPrompt(
                title = stringResource(R.string.confirming),
                busy = state.busy, locked = state.locked, message = state.message,
                onRetry = { viewModel.confirmSystem(onConfirmed, onForcedMaster) },
            )
        }
        UnlockMethod.Safe -> SafeDial(
            title = stringResource(R.string.confirm_safe),
            subtitle = state.message,
            enabled = !state.busy,
            resetKey = state.resetKey,
            onComplete = { viewModel.confirmPin(it, onConfirmed, onForcedMaster) }
        )
    }
}