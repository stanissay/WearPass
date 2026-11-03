package stanissay.wear.pass.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.UiConstants

@Composable
fun GroupScreen(groupId: String?, onGroup: (String) -> Unit, onEntry: (String) -> Unit) {
    val session = (LocalContext.current.applicationContext as App).session
    val view = remember(groupId) { session.index?.group(groupId) }
    val syncText = if (groupId == null) when (session.lastSync) {
        SyncNote.Updated -> stringResource(R.string.sync_updated)
        SyncNote.SourceMissing -> stringResource(R.string.sync_source_missing)
        SyncNote.SourceUnreadable -> stringResource(R.string.sync_source_unreadable)
        SyncNote.None -> null
    } else null

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { PageTitle(view?.name.orEmpty()) } }

        syncText?.let { item { MainText(it, style = MaterialTheme.typography.labelMedium) } }

        if (view == null) {
            item { MainText(stringResource(R.string.no_group)) }
        } else {
            items(view.groups, key = { "g-${it.id}" }) { g ->
                ClickableBox(
                    onClick = { onGroup(g.id) },
                    modifier = Modifier.fillMaxWidth().height(UiConstants.BASE_SIZE),
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(PaddingStyle.horizontalXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
                    ) {
                        MainText("${g.entryCount}")
                        MainText(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            items(view.entries, key = { "e-${it.id}" }) { e ->
                ClickableBox(
                    onClick = { onEntry(e.id) },
                    modifier = Modifier.fillMaxWidth().height(UiConstants.BASE_SIZE),
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(PaddingStyle.horizontalXs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MainText(e.title.ifEmpty { stringResource(R.string.unnamed) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (e.userName.isNotEmpty()) {
                            MainText(e.userName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (view.groups.isEmpty() && view.entries.isEmpty()) item { MainText(stringResource(R.string.empty)) }
        }
    }
}