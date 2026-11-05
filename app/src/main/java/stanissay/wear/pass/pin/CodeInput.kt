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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.UiConstants
import stanissay.wear.pass.utils.UnlockMethod

@Composable
fun CodeInput(
    method: UnlockMethod,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    resetKey: Any,
    onComplete: (CharArray) -> Unit,
) {
    Box(Modifier.fillMaxSize().blur(if (!enabled) UiConstants.BASE_SIZE_XXS else 0.dp)) {
        if (method == UnlockMethod.Safe) SafeDial(title, subtitle, enabled, resetKey, onComplete)
        else PinPad(title, subtitle, enabled, resetKey, onComplete)
    }
    if (!enabled) CircularBar()
}

@Composable
fun CodeSetupScreen(
    method: UnlockMethod,
    onDone: () -> Unit,
    viewModel: PinSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    CodeInput(
        method = method,
        title = if (method == UnlockMethod.Safe) stringResource(if (!state.confirming) R.string.create_safe else R.string.confirm_safe)
        else if (!state.confirming) stringResource(R.string.create_pin) else stringResource(R.string.confirm_pin),
        subtitle = state.error,
        enabled = !state.saving,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(method, it, onDone) }
    )
}

@Composable
fun CodeUnlockScreen(
    method: UnlockMethod,
    onUnlocked: () -> Unit,
    onForcedMaster: () -> Unit,
    viewModel: PinUnlockViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    CodeInput(
        method = method,
        title = if (method == UnlockMethod.Safe) stringResource(R.string.safe) else stringResource(R.string.pin),
        subtitle = state.error,
        enabled = !state.busy,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(method, it, onUnlocked, onForcedMaster) }
    )
}