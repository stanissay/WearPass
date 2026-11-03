package stanissay.wear.pass.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.UiConstants
import kotlin.time.Duration.Companion.milliseconds

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
                    password?.let { p -> Text(p, fontFamily = FontFamily.Monospace) }
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
        MainText(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.surfaceContainer)
        MainText(value)
    }
}