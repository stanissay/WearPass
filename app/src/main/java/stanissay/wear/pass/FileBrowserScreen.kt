package stanissay.wear.pass

import android.os.Environment
import android.text.format.Formatter
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.MainConstants
import stanissay.wear.pass.utils.MainText

@Composable
fun FileBrowserScreen(
    onImported: () -> Unit,
    vm: FileBrowserViewModel = viewModel(),
) {
    val clipboard = LocalClipboard.current
    val ui by vm.state.collectAsState()
    val pkg = LocalContext.current.packageName
    val title = if (ui.dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath) "/" else ui.dir.name
    val adbCommand = "adb shell appops set $pkg MANAGE_EXTERNAL_STORAGE allow"

    BackHandler(enabled = vm.canGoUp) { vm.up() }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { MainText(title) } }

        if (!ui.hasAccess) {
            item {
                ClickableBox(
                    onLongClick = { vm.copyText(clipboard, adbCommand) }
                ) { MainText("Немає доступу до файлів. Виконай:\n$adbCommand") }
            }
        }

        if (vm.canGoUp) {
            item { Button(onClick = vm::up, modifier = Modifier.fillMaxWidth(), label = { MainText("..") }) }
        }

        items(ui.items, key = { it.absolutePath }) { f ->
            Button(
                onClick = { if (f.isDirectory) vm.open(f) else vm.import(f, onImported) },
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth(),
                label = { MainText(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = {
                    if(f.isDirectory) {
                        Image(
                            painter = painterResource(R.drawable.folder_ic),
                            contentDescription = "directory",
                            modifier = Modifier.size(MainConstants.BUTTON_SIZE_S)
                        )
                    }
                }
            )
        }

        ui.error?.let { item { MainText(it) } }
    }
}