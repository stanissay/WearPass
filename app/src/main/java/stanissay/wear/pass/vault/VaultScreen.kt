package stanissay.wear.pass.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.SubTitle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.UiConstants
import kotlin.time.Duration.Companion.milliseconds

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
                        EntryIcon(g.customIconId, g.name)
                        SubTitle(g.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        MainText("${g.entryCount}")
                    }
                }
            }
            items(view.entries, key = { "e-${it.id}" }) { e ->
                ClickableBox(
                    onClick = { onEntry(e.id) },
                    modifier = Modifier.fillMaxWidth().height(UiConstants.BASE_SIZE),
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(PaddingStyle.horizontalXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
                    ) {
                        EntryIcon(e.customIconId, e.title)
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SubTitle(e.title.ifEmpty { stringResource(R.string.unnamed) })
                            if (e.userName.isNotEmpty()) MainText(e.userName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (view.groups.isEmpty() && view.entries.isEmpty()) item { MainText(stringResource(R.string.empty)) }
        }
    }
}

@Composable
fun EntryScreen(entryId: String) {
    val session = (LocalContext.current.applicationContext as App).session
    val entry = remember(entryId) { session.index?.entry(entryId) }
    var password by remember(entryId) { mutableStateOf<String?>(null) }

    LaunchedEffect(password) {
        if (password != null) {
            delay(DataConstants.PASSWORD_VISIBLE_MS.milliseconds)
            password = null
        }
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { EntryIcon(entry?.customIconId, entry?.title) } }
        item { ListHeader { PageTitle(entry?.title?.ifEmpty { stringResource(R.string.unnamed) }.orEmpty()) } }

        if (entry == null) {
            item { MainText(stringResource(R.string.no_entry)) }
        } else {
            if (entry.userName.isNotEmpty()) item { Field(stringResource(R.string.login), entry.userName) }
            item {
                ClickableBox(
                    modifier = Modifier.fillMaxWidth().height(UiConstants.BASE_SIZE),
                    borderColor = MaterialTheme.colorScheme.outline,
                    onClick = { password = if (password == null) session.index?.password(entryId) else null }
                ) {
                    password?.let { p -> MainText(p, style = MaterialTheme.typography.bodyLarge) }
                        ?: MainText(stringResource(R.string.show_password))
                }
            }
            if (entry.url.isNotEmpty()) item { Field(stringResource(R.string.URL), entry.url) }
            if (entry.notes.isNotEmpty()) item { Field(stringResource(R.string.notes), entry.notes) }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(PaddingStyle.horizontalXs)) {
        SubTitle(label)
        MainText(value, textAlign = TextAlign.Start)
    }
}

@Composable
fun EntryIcon(customIconId: String?, title: String?, modifier: Modifier = Modifier) {
    val session = (LocalContext.current.applicationContext as App).session
    val bitmap by produceState<ImageBitmap?>(null, customIconId) {
        value = customIconId?.let { id ->
            withContext(Dispatchers.Default) { session.index?.customIcon(id) }
        }
    }
    val bmp = bitmap

    Box(
        modifier = modifier.size(UiConstants.BUTTON_SIZE)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                val letter = title?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
                MainText(letter, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}