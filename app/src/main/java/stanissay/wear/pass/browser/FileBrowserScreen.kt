/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass.browser

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.BrowserMode
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.UiConstants
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PageTitle

@Composable
fun FileBrowserScreen(onImported: () -> Unit) {
    val viewModel: FileBrowserViewModel = viewModel()
    val clipboard = LocalClipboard.current
    val packageName = LocalContext.current.packageName
    val state by viewModel.state.collectAsState()
    val title = if (state.dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath) DataConstants.BROWSER_ROOT else state.dir.name
    val adbCommand = "adb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow"

    BackHandler(enabled = viewModel.canGoUp) { viewModel.up() }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { PageTitle(title) } }

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
            item { ClickableListItem(onClick = viewModel::up) { MainText(DataConstants.BROWSER_UP) } }

        items(state.items, key = { it.absolutePath }) { f ->
            ClickableListItem(
                onClick = {
                    if(state.mode == BrowserMode.Importing) return@ClickableListItem
                    if (f.isDirectory) viewModel.open(f) else viewModel.import(f, onImported)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize()
                        .padding(UiConstants.paddingH),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
                ) {
                    if(f.isDirectory) {
                        Image(
                            painter = painterResource(R.drawable.folder_ic),
                            contentDescription = "directory",
                            modifier = Modifier.size(UiConstants.BUTTON_SIZE_S),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    MainText(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}