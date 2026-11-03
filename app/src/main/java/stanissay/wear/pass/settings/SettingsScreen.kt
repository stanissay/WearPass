package stanissay.wear.pass.settings

import android.app.KeyguardManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.vault.VaultStatus
import java.io.File

@Composable
fun SettingsScreen(
    onImport: () -> Unit,
    onWiped: () -> Unit,
    onLockNow: () -> Unit,
    onSwitchTo: (UnlockMethod) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val unlocked by app.session.unlocked.collectAsState()
    val status by app.vault.status.collectAsState()
    val method = remember { app.vault.unlockMethod }
    val systemAvailable = remember { context.getSystemService(KeyguardManager::class.java).isDeviceSecure }
    val source = remember { app.vault.sourcePath?.let { File(it).name } }
    var confirmWipe by remember { mutableStateOf(false) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { PageTitle(stringResource(R.string.settings)) } }

        if (unlocked) {
            item { Button(onClick = onLockNow, modifier = Modifier.fillMaxWidth(), label = { MainText(stringResource(R.string.lock_now)) }) }

            item { Section(stringResource(R.string.lock_type)) }
            item {
                Choice(
                    selected = method != UnlockMethod.SystemLock,
                    label = stringResource(R.string.pin),
                    secondary = stringResource(R.string.pin_safe),
                ) { if (method == UnlockMethod.SystemLock) onSwitchTo(UnlockMethod.Pin) }
            }
            item {
                Choice(
                    selected = method == UnlockMethod.SystemLock,
                    label = stringResource(R.string.system_lock),
                    secondary = if (systemAvailable) stringResource(R.string.while_unlocked) else stringResource(R.string.setup_lock),
                    enabled = systemAvailable,
                ) { if (method != UnlockMethod.SystemLock) onSwitchTo(UnlockMethod.SystemLock) }
            }

            if (method != UnlockMethod.SystemLock) {
                item { Section(stringResource(R.string.pin_type)) }
                item {
                    Choice(method == UnlockMethod.Pin, stringResource(R.string.classic_pin), stringResource(R.string.classic_pin_description)) {
                        if (method != UnlockMethod.Pin) onSwitchTo(UnlockMethod.Pin)
                    }
                }
                item {
                    Choice(method == UnlockMethod.Safe, stringResource(R.string.safe), stringResource(R.string.safe_description)) {
                        if (method != UnlockMethod.Safe) onSwitchTo(UnlockMethod.Safe)
                    }
                }
            }
        } else if (status != VaultStatus.NotImported) {
            item { Section(stringResource(R.string.base_locked)) }
        }

        item { Section(stringResource(R.string.data)) }
        item {
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                label = { MainText(stringResource(R.string.import_new_file)) },
                secondaryLabel = { MainText(source?.let { "${stringResource(R.string.current)} $it" } ?: stringResource(R.string.change_base), maxLines = 1) },
            )
        }

        if (status != VaultStatus.NotImported) {
            if (!confirmWipe) {
                item { Danger(stringResource(R.string.delete_all)) { confirmWipe = true } }
            } else {
                item { MainText(stringResource(R.string.delete_question)) }
                item { Danger(stringResource(R.string.delete)) { app.wipeAll(); onWiped() } }
                item {
                    Button(
                        onClick = { confirmWipe = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { MainText(stringResource(R.string.cancel)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) = MainText(
    text,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(PaddingStyle.xs),
)

@Composable
private fun Choice(
    selected: Boolean,
    label: String,
    secondary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth(),
    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
    icon = { MainText(if (selected) "●" else "○") },
    label = { MainText(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    secondaryLabel = secondary?.let { s -> { MainText(s, maxLines = 2) } },
)

@Composable
private fun Danger(text: String, onClick: () -> Unit) = Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ),
    label = { MainText(text) },
)