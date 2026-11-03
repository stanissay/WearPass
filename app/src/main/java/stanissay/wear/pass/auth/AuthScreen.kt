package stanissay.wear.pass.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import stanissay.wear.pass.R
import stanissay.wear.pass.pin.PinPad
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.sl.SystemPrompt
import stanissay.wear.pass.utils.MainText

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
        UnlockMethod.Safe -> MainText("Сейф ще не реалізований")
    }
}