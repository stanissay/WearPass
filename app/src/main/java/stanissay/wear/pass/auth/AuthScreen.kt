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
    vm: AuthViewModel = viewModel(),
) {
    val ui by vm.state.collectAsState()
    when (vm.method) {
        UnlockMethod.Pin -> PinPad(
            title = stringResource(R.string.confirm_pin),
            subtitle = ui.message,
            enabled = !ui.busy,
            resetKey = ui.resetKey,
            onComplete = { vm.confirmPin(it, onConfirmed, onForcedMaster) },
        )
        UnlockMethod.SystemLock -> {
            LifecycleResumeEffect(Unit) {
                vm.confirmSystem(onConfirmed, onForcedMaster)
                onPauseOrDispose { }
            }
            SystemPrompt(
                title = stringResource(R.string.confirming),
                busy = ui.busy, locked = ui.locked, message = ui.message,
                onRetry = { vm.confirmSystem(onConfirmed, onForcedMaster) },
            )
        }
        UnlockMethod.Safe -> MainText("Сейф ще не реалізований")
    }
}