package stanissay.wear.pass.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R
import stanissay.wear.pass.pin.PinPad
import stanissay.wear.pass.pin.PinSetupViewModel
import stanissay.wear.pass.pin.PinUnlockViewModel
import stanissay.wear.pass.safe.SafeDial
import stanissay.wear.pass.settings.UnlockMethod

@Composable
fun CodeInput(
    method: UnlockMethod,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    resetKey: Any,
    onComplete: (CharArray) -> Unit,
) {
    if (method == UnlockMethod.Safe) {
        SafeDial(title, subtitle, enabled, resetKey, onComplete)
    } else {
        PinPad(
            title = title, subtitle = subtitle, enabled = enabled,
            resetKey = resetKey, onComplete = onComplete,
        )
    }
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