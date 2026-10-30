package stanissay.wear.pass

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import stanissay.wear.pass.utils.MainText

@Composable
fun FileBrowserScreen(
    onImported: () -> Unit,
    vm: FileBrowserViewModel = viewModel(),
) {
    val ui by vm.state.collectAsState()
    val pkg = LocalContext.current.packageName

    BackHandler(enabled = vm.canGoUp) { vm.up() }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { MainText(ui.dir.name.ifEmpty { "/" }, maxLines = 1) } }

        if (!ui.hasAccess) {
            item {
                MainText("Немає доступу до файлів. Виконай:\nadb shell appops set $pkg MANAGE_EXTERNAL_STORAGE allow")
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
                secondaryLabel = {
                    MainText(if (f.isDirectory) "папка" else Formatter.formatShortFileSize(LocalContext.current, f.length()))
                },
            )
        }

        item {
            Button(
                onClick = vm::toggleShowAll,
                modifier = Modifier.fillMaxWidth(),
                label = { MainText(if (ui.showAll) "Лише .kdbx" else "Усі файли") },
            )
        }

        ui.error?.let { item { MainText(it) } }
    }
}