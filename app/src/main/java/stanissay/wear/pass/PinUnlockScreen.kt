package stanissay.wear.pass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PinUnlockScreen(
    onUnlocked: () -> Unit,
    onForcedMaster: () -> Unit,
    vm: PinUnlockViewModel = viewModel(),
) {
    val ui by vm.state.collectAsState()
    PinPad(
        title = "PIN",
        subtitle = ui.error,
        enabled = !ui.busy,
        resetKey = ui.resetKey,
        onComplete = { vm.onPin(it, onUnlocked, onForcedMaster) },
    )
}