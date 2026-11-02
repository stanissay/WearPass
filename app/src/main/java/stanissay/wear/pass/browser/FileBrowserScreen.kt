package stanissay.wear.pass.browser

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.UiConstants
import stanissay.wear.pass.utils.MainText

@Composable
fun FileBrowserScreen(onImported: () -> Unit) {
    val viewModel: FileBrowserViewModel = viewModel()
    val clipboard = LocalClipboard.current
    val packageName = LocalContext.current.packageName
    val state by viewModel.state.collectAsState()
    val title = if (state.dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath) "/" else state.dir.name
    val adbCommand = "adb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow"

    BackHandler(enabled = viewModel.canGoUp) { viewModel.up() }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { MainText(title) } }

        when (val mode = state.mode) {
            BrowserMode.NoAccess -> item {
                ClickableBox(
                    onLongClick = { viewModel.copyText(clipboard, adbCommand) }
                ) { MainText("${stringResource(R.string.no_access)}\n$adbCommand") }
            }
            BrowserMode.Importing -> item { CircularBar() }
            is BrowserMode.Failed -> item { MainText(mode.message) }
            BrowserMode.Idle -> Unit
        }

        if (viewModel.canGoUp)
            item { Button(onClick = viewModel::up, modifier = Modifier.fillMaxWidth(), label = { MainText(DataConstants.BROWSER_UP) }) }

        items(state.items, key = { it.absolutePath }) { f ->
            Button(
                onClick = { if (f.isDirectory) viewModel.open(f) else viewModel.import(f, onImported) },
                enabled = state.mode != BrowserMode.Importing,
                modifier = Modifier.fillMaxWidth(),
                label = { MainText(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = {
                    if(f.isDirectory) {
                        Image(
                            painter = painterResource(R.drawable.folder_ic),
                            contentDescription = "directory",
                            modifier = Modifier.size(UiConstants.BUTTON_SIZE_S)
                        )
                    }
                }
            )
        }
    }
}