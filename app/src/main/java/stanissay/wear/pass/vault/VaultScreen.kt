package stanissay.wear.pass.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.material3.TimeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.MainDivider
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.Revealed
import stanissay.wear.pass.utils.SubTitle
import stanissay.wear.pass.utils.UiConstants
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GroupScreen(groupId: String?, onGroup: (String) -> Unit, onEntry: (String) -> Unit) {
    val session = (LocalContext.current.applicationContext as App).session
    val view = remember(groupId) { session.index?.group(groupId) }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        view?.let { v ->
            item { ListHeader { PageTitle(view.name) } }
            items(v.groups, key = { "g-${it.id}" }) { g ->
                ClickableListItem(onClick = { onGroup(g.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PaddingStyle.horizontalXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
                    ) {
                        EntryIcon(g.icon, g.name)
                        SubTitle(g.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        MainText("${g.entryCount}", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            items(v.entries, key = { "e-${it.id}" }) { e ->
                ClickableListItem(onClick = { onEntry(e.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PaddingStyle.horizontalXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
                    ) {
                        EntryIcon(e.icon, e.title)
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SubTitle(e.title.ifEmpty { stringResource(R.string.unnamed) })
                            if (e.userName.isNotEmpty()) MainText(
                                text = e.userName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            if (v.groups.isEmpty() && v.entries.isEmpty()) item { MainText(stringResource(R.string.empty)) }
        } ?: item { MainText(stringResource(R.string.no_group)) }
    }
}

@Composable
fun EntryScreen(entryId: String) {
    val session = (LocalContext.current.applicationContext as App).session
    val entry = remember(entryId) { session.index?.entry(entryId) }
    var revealed by remember(entryId) { mutableStateOf<Revealed?>(null) }

    LaunchedEffect(revealed?.id) {
        if (revealed != null) {
            delay(DataConstants.PASSWORD_VISIBLE_MS.milliseconds)
            revealed = null
        }
    }

    fun toggle(id: String, load: () -> String?) {
        revealed = if (revealed?.id == id) null else load()?.let { Revealed(id, it) }
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        entry?.let { e ->
            item { EntryIcon(e.icon, e.title) }
            item { ListHeader { PageTitle(e.title) } }
            if (e.userName.isNotEmpty()) item { Field(stringResource(R.string.login), e.userName) }
            if (e.hasPassword) {
                item {
                    val shown = revealed?.id == DataConstants.PASSWORD
                    ClickableListItem(onClick = {
                        toggle(DataConstants.PASSWORD) {
                            session.index?.password(
                                entryId
                            )
                        }
                    }) {
                        if (shown) {
                            MainText(revealed!!.value, style = MaterialTheme.typography.bodyLarge)
                        } else MainText(stringResource(R.string.password))
                    }
                }
            }
            if (e.url.isNotEmpty()) item { Field(stringResource(R.string.URL), e.url) }
            if (e.notes.isNotEmpty()) item { Field(stringResource(R.string.notes), e.notes) }

            if(entry.customFields.isNotEmpty()) {
                item { MainDivider() }
                entry.customFields.forEach { f ->
                    if (!f.isProtected) {
                        if (!f.value.isNullOrEmpty()) item { Field(f.name, f.value) }
                    } else {
                        val id = "cf:${f.name}"
                        val shown = revealed?.id == id
                        item {
                            ClickableListItem(onClick = { toggle(id) { session.index?.customFieldValue(entryId, f.name) } }) {
                                if(shown) {
                                    MainText(revealed!!.value, style = MaterialTheme.typography.bodyLarge)
                                } else MainText(f.name)
                            }
                        }
                    }
                }
            }

            if (entry.attachments.isNotEmpty()) {
                item { MainDivider() }
                item { SubTitle(stringResource(R.string.attachments)) }
                items(entry.attachments) { name -> MainText(name) }
            }
        } ?: item { MainText(stringResource(R.string.no_entry)) }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(Modifier
        .fillMaxWidth()
        .padding(PaddingStyle.horizontalXs)) {
        SubTitle(label)
        MainText(value, textAlign = TextAlign.Start)
    }
}

@Composable
fun EntryIcon(icon: IconRef, title: String?, modifier: Modifier = Modifier, hideDefaultKey: Boolean = false) {
    val app = LocalContext.current.applicationContext as App
    val bitmap by produceState<ImageBitmap?>(null, icon) {
        value = withContext(Dispatchers.Default) {
            icon.customId?.let { app.session.index?.customIcon(it) }
                ?: icon.builtIn
                    ?.takeUnless { hideDefaultKey && it == 0 }
                    ?.let { app.builtInIcons.get(it) }
        }
    }
    val bmp = bitmap

    Box(
        modifier = modifier.size(UiConstants.BUTTON_SIZE).clip(MaterialTheme.shapes.extraSmall),
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
                    .background(MaterialTheme.colorScheme.secondary)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val letter = title?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
                MainText(letter, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}