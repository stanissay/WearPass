package stanissay.wear.pass.pin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R

@Composable
fun PinSetupScreen(
    onDone: () -> Unit,
    onSettings: () -> Unit,
    viewModel: PinSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    PinPad(
        title = if (!state.confirming) stringResource(R.string.create_pin) else stringResource(R.string.confirm_pin),
        subtitle = state.error,
        enabled = !state.saving,
        resetKey = state.resetKey,
        onComplete = { viewModel.onPin(it, onDone) },
        onSettings = onSettings
    )
}