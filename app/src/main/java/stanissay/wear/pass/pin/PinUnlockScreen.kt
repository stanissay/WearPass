package stanissay.wear.pass.pin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R

@Composable
fun PinUnlockScreen(
    onUnlocked: () -> Unit,
    onForcedMaster: () -> Unit,
    viewModel: PinUnlockViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    PinPad(
        title = stringResource(R.string.pin),
        subtitle = state.error,
        enabled = !state.busy,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(it, onUnlocked, onForcedMaster) }
    )
}