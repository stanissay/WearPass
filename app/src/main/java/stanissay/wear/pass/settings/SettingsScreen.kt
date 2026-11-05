package stanissay.wear.pass.settings

import android.app.KeyguardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.App
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.MainCheckBox
import stanissay.wear.pass.utils.MainColors
import stanissay.wear.pass.utils.MainDivider
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.SubTitle
import stanissay.wear.pass.utils.UiConstants
import stanissay.wear.pass.utils.openCredentialSettings
import stanissay.wear.pass.vault.VaultStatus

@Composable
fun SettingsScreen(
    onImport: () -> Unit,
    onWiped: () -> Unit,
    onLockNow: () -> Unit,
    onSwitchTo: (UnlockMethod) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
    val unlocked by app.session.unlocked.collectAsState()
    val status by app.vault.status.collectAsState()
    val method = remember { app.vault.unlockMethod }
    val systemAvailable = remember { context.getSystemService(KeyguardManager::class.java).isDeviceSecure }
    var confirmWipe by remember { mutableStateOf(false) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { PageTitle(stringResource(R.string.settings)) } }

        if (unlocked) {
            if (method != UnlockMethod.SystemLock) item { ClickableListItem(onClick = onLockNow) { MainText(stringResource(R.string.lock_now)) } }
            item { ClickableListItem(onClick = { openCredentialSettings(context) }) { MainText(stringResource(R.string.autofill)) } }
            item { MainDivider() }
            item { SubTitle(stringResource(R.string.lock_type)) }
            item {
                Choice(
                    selected = method != UnlockMethod.SystemLock,
                    label = stringResource(R.string.pin)
                ) { if (method == UnlockMethod.SystemLock) onSwitchTo(UnlockMethod.Pin) }
            }
            item {
                Choice(
                    selected = method == UnlockMethod.SystemLock,
                    label = stringResource(R.string.system_lock),
                    enabled = systemAvailable,
                ) { if (method != UnlockMethod.SystemLock) onSwitchTo(UnlockMethod.SystemLock) }
            }

            if (method != UnlockMethod.SystemLock) {
                item { SubTitle(stringResource(R.string.pin_type)) }
                item {
                    Choice(method == UnlockMethod.Pin, stringResource(R.string.classic_pin)) {
                        if (method != UnlockMethod.Pin) onSwitchTo(UnlockMethod.Pin)
                    }
                }
                item {
                    Choice(method == UnlockMethod.Safe, stringResource(R.string.safe)) {
                        if (method != UnlockMethod.Safe) onSwitchTo(UnlockMethod.Safe)
                    }
                }
            }
        } else if (status != VaultStatus.NotImported)
            item { MainText(stringResource(R.string.vault_locked)) }

        item { MainDivider() }

        item { SubTitle(stringResource(R.string.data)) }
        item {
            ClickableListItem(onClick = onImport) { MainText(stringResource(R.string.import_new_file)) }
        }

        if (status != VaultStatus.NotImported) {
            if(confirmWipe) item { MainText(stringResource(R.string.delete_question)) }
            item {
                Danger(stringResource(R.string.delete)) {
                    if(!confirmWipe) confirmWipe = true else { app.wipeAll(); onWiped() }
                }
            }
            if(confirmWipe) item { ClickableListItem(onClick = { confirmWipe = false }) { MainText(stringResource(R.string.cancel)) } }
        }
        item {
            MainText(
                text = "${stringResource(R.string.version)} $versionName",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun Choice(
    selected: Boolean,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = ClickableListItem(
    onClick = {
        if(!enabled) return@ClickableListItem
        onClick()
    }
) {
    Row(
        modifier = Modifier.fillMaxSize()
            .padding(UiConstants.paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
    ) {
        MainCheckBox(check = selected)
        MainText(label)
    }
}

@Composable
private fun Danger(text: String, onClick: () -> Unit) = ClickableBox(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(UiConstants.BASE_SIZE),
    background = MainColors.NEGATIVE
) { MainText(text) }