package stanissay.wear.pass.pin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.utils.codeLength

@Composable
fun PinSetupScreen(
    method: UnlockMethod,
    onDone: () -> Unit,
    viewModel: PinSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    PinPad(
        title = if (method == UnlockMethod.Safe) stringResource(if (!state.confirming) R.string.create_safe else R.string.confirm_safe)
        else if (!state.confirming) stringResource(R.string.create_pin) else stringResource(R.string.confirm_pin),
        length = codeLength(method),
        subtitle = state.error,
        enabled = !state.saving,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(method, it, onDone) }
    )
}