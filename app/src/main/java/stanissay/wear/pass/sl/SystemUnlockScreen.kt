package stanissay.wear.pass.sl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PageTitle

@Composable
fun SystemUnlockScreen(
    onUnlocked: () -> Unit,
    onForcedMaster: () -> Unit,
    vm: SystemUnlockViewModel = viewModel(),
) {
    val ui by vm.state.collectAsState()
    LifecycleResumeEffect(Unit) {
        vm.tryUnlock(onUnlocked, onForcedMaster)
        onPauseOrDispose { }
    }
    SystemPrompt(
        title = stringResource(R.string.unlocking),
        busy = ui.busy, locked = ui.locked, message = ui.message,
        onRetry = { vm.tryUnlock(onUnlocked, onForcedMaster) },
        onMaster = onForcedMaster,
    )
}

@Composable
fun SystemPrompt(
    title: String,
    busy: Boolean,
    locked: Boolean,
    message: String?,
    onRetry: () -> Unit,
    onMaster: (() -> Unit)? = null,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { PageTitle(title) } }
        if (busy) item { CircularBar() }
        if (locked) item { MainText(stringResource(R.string.unlock_watch)) }
        message?.let { m -> item { MainText(m, style = MaterialTheme.typography.labelMedium) } }
        item { ClickableListItem(onClick = onRetry) { MainText(stringResource(R.string.open)) } }
        onMaster?.let { go ->
            item { ClickableListItem(onClick = go) { MainText(stringResource(R.string.master_password)) } }
        }
    }
}