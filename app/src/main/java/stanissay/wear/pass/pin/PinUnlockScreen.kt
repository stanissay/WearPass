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
fun PinUnlockScreen(
    method: UnlockMethod,
    onUnlocked: () -> Unit,
    onForcedMaster: () -> Unit,
    viewModel: PinUnlockViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    PinPad(
        title = if (method == UnlockMethod.Safe) stringResource(R.string.safe) else stringResource(R.string.pin),
        length = codeLength(method),
        subtitle = state.error,
        enabled = !state.busy,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(method, it, onUnlocked, onForcedMaster) }
    )
}