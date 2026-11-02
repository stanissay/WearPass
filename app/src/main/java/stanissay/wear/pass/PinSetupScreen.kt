package stanissay.wear.pass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PinSetupScreen(onDone: () -> Unit, vm: PinSetupViewModel = viewModel()) {
    val ui by vm.state.collectAsState()
    PinPad(
        title = if (ui.confirming) "Повтори PIN" else "Створи PIN",
        subtitle = ui.error,
        enabled = !ui.saving,
        resetKey = ui.resetKey,
        onComplete = { vm.onPin(it, onDone) },
    )
}