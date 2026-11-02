package stanissay.wear.pass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import stanissay.wear.pass.browser.FileBrowserScreen
import stanissay.wear.pass.master.MasterPasswordScreen
import stanissay.wear.pass.pin.PinSetupScreen
import stanissay.wear.pass.pin.PinUnlockScreen
import stanissay.wear.pass.utils.EntryKey
import stanissay.wear.pass.utils.FileBrowserKey
import stanissay.wear.pass.utils.GroupKey
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.MasterPasswordKey
import stanissay.wear.pass.utils.PinSetupKey
import stanissay.wear.pass.utils.PinUnlockKey
import stanissay.wear.pass.utils.SettingsKey
import stanissay.wear.pass.utils.VaultStatus

@Composable
fun AppNav(vault: VaultRepository) {
    val start = remember {
        when (vault.status.value) {
            VaultStatus.NotImported -> FileBrowserKey
            VaultStatus.NeedsMasterPassword -> MasterPasswordKey
            VaultStatus.PinLocked -> PinUnlockKey
        }
    }
    val backStack = rememberNavBackStack(start)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider = entryProvider {
            entry<FileBrowserKey> {
                FileBrowserScreen(onImported = {
                    backStack.clear(); backStack.add(MasterPasswordKey)
                })
            }
            entry<MasterPasswordKey> {
                MasterPasswordScreen(onOpened = { backStack.clear(); backStack.add(PinSetupKey) })
            }
            entry<PinSetupKey> {
                PinSetupScreen(onDone = { backStack.clear(); backStack.add(GroupKey()) })
            }
            entry<PinUnlockKey> {
                PinUnlockScreen(
                    onUnlocked = { backStack.clear(); backStack.add(GroupKey()) },
                    onForcedMaster = { backStack.clear(); backStack.add(MasterPasswordKey) },
                )
            }
            entry<GroupKey> { Stub("Групи й записи") }
            entry<EntryKey> { Stub("Запис") }
            entry<SettingsKey> { Stub("Налаштування") }
        },
    )
}

@Composable private fun Stub(title: String) = Box(Modifier.fillMaxSize(), Alignment.Center) { MainText(title) }