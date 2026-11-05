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

package stanissay.wear.pass.lock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PageTitle

@Composable
fun SystemSetupScreen(
    onDone: () -> Unit,
    vm: SystemSetupViewModel = viewModel(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val available = remember { app.systemVault.isAvailable(context) }
    val ui by vm.state.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { PageTitle(stringResource(R.string.system_lock)) } }
        ui.error?.let { e -> item { MainText(e, style = MaterialTheme.typography.labelMedium) } }
        if (available) {
            item {
                ClickableListItem(
                    onClick = {
                        if(ui.busy) return@ClickableListItem
                        vm.enable(onDone)
                    }
                ) { MainText(stringResource(R.string.on)) }
            }
        } else item { MainText(stringResource(R.string.create_pin)) }
    }
}